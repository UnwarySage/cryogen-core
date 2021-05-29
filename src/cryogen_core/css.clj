(ns cryogen-core.css)


(defonce css-processor-registry (atom nil))


(defprotocol CssProcessor 
  "A css processor that consists of a set of extensions, (ext)
   an expected directory inside a theme(dir), and a function process,
   with the signature [java.IO.file output-location config] and renders css in the dir to the ouput file as a side effect"
  (ext [] "the set of extensions that the processor can handle")
  (dir [] "the expected directory name in the theme directory to look at for files.")
  (process [inp-dir output-file config] "takes an inp dir of files, an output file name, and the config and the site config, and expected to compile css as a side effect"))

(defn processor
  "Return a vector of css-processer implementations. This is the primary entry point
  for a client of this ns. This vector should be used to iterate over supported
  processors."
  []
  @css-processor-registry)

(defn set-processor!
  "Sets the new processor"
  [new-processor]
  (reset! css-processor-registry new-processor))

(defn clear-processor!
  "clears all processors from the registry"
  []
  (reset! css-processor-registry nil))