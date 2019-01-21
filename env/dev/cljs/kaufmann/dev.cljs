(ns ^:figwheel-no-load kaufmann.dev
  (:require
    [kaufmann.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
