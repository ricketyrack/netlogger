(ns ^:figwheel-no-load netlogger.dev
  (:require
    [netlogger.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
