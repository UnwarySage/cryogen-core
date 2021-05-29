(ns cryogen-core.config
  (:require [clojure.string :as string]
            [schema.core :as s]
            [cryogen-core.schemas :as schemas]
            [cryogen-core.io :as cryogen-io]))

(defn subpath?
  "Checks if either path is a subpath of the other"
  [p1 p2]
  (let [parts #(string/split % #"/")]
    (every? #{true} (map #(= %1 %2) (parts p1) (parts p2)))))

(defn root-uri
  "Creates the uri for posts and pages. Returns root-path by default"
  [k config]
  (if-let [uri (k config)]
    uri
    (config (-> k (name) (string/replace #"-uri$" "") (keyword)))))

(defn sass-or-post-switch
  "Given a config, looks for what process should handle it CSS compilation,
   and sets up the expected values for later processing."
  [{:keys [css-src css-compiler] :as config}]
  (if css-compiler ;; maybe there is no css to compile
    (cond (= :sass css-compiler)
          (assoc config
                 :sass-src css-src)
          (= :post-css css-compiler)
          (assoc config
                 :post-css-src css-src))
    config))

(defn process-config
  "Reads the config file"
  [config]
  (try
    (s/validate schemas/Config config)
    (let [config (-> config
                     (update-in [:tag-root-uri] (fnil identity ""))
                     (update-in [:public-dest] (fnil identity "public"))
                     (update-in [:recent-posts] (fnil identity 3))
                     (update-in [:archive-group-format] (fnil identity "yyyy MMMM"))
                     (update-in [:css-src] (fnil identity ["css"]))
                     (update-in [:sass-path] (fnil identity "sass"))
                     (update-in [:posts-per-page] (fnil identity 5))
                     (update-in [:blocks-per-preview] (fnil identity 2))
                     (assoc :page-root-uri (root-uri :page-root-uri config)
                            :post-root-uri (root-uri :post-root-uri config))
                     (sass-or-post-switch))
          check-overlap (fn [dirs]
                          (some #(subpath? % (:public-dest config)) dirs))]
      
      (if (or (= (string/trim (:public-dest config)) "")
              (string/starts-with? (:public-dest config) ".")
              (check-overlap ["content" "themes" "src" "target"]))
        (throw (new Exception "Dangerous :public-dest value. The folder will be deleted each time the content is rendered. Specify a sub-folder that doesn't overlap with the default folders or your resource folders."))
        config))
    (catch Exception e (throw e))))

(defn deep-merge
  "Recursively merges maps. When override is true, for scalars and vectors,
  the last value wins. When override is false, vectors are merged, but for
  scalars, the last value still wins."
  [override & vs]
  (cond
    (= (count vs) 1) vs
    (every? map? vs) (apply merge-with (partial deep-merge override) vs)
    (and (not override) (every? sequential? vs)) (apply into vs)
    :else (last vs)))



(defn add-theme-css-dirs 
  [config theme-config]
  (assoc config
         :theme-resources
         (or (:resources theme-config) [])
         :theme-sass-src
         (or (:sass-src theme-config) [])
         :theme-post-css-src
         (or (:post-css-src theme-config) [])))

(defn read-config []
  (let [config (-> "content/config.edn"
                   cryogen-io/get-resource
                   cryogen-io/read-edn-resource)
        theme-config-resource (-> config
                                  :theme
                                  (#(cryogen-io/path "themes" % "config.edn"))
                                  cryogen-io/get-resource)
        theme-config (if theme-config-resource
                       (cryogen-io/read-edn-resource theme-config-resource))]
    (add-theme-css-dirs config theme-config)))

(defn resolve-config
  "Loads the config file, merging in the overrides and, and filling in missing defaults"
  ([]
   (resolve-config {}))
  ([overrides]
   (process-config (deep-merge true (read-config) overrides))))
