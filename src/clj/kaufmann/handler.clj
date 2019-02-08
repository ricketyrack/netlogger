(ns kaufmann.handler
  (:require [reitit.ring :as reitit-ring]
            [kaufmann.middleware :refer [middleware]]
            [hiccup.page :refer [include-js include-css html5]]
            [config.core :refer [env]]))

(def mount-target
  [:div#zz
   [:div#app]
   [:div.sk-cube-grid
    [:div.sk-cube.sk-cube1]
    [:div.sk-cube.sk-cube2]
    [:div.sk-cube.sk-cube3]
    [:div.sk-cube.sk-cube4]
    [:div.sk-cube.sk-cube5]
    [:div.sk-cube.sk-cube6]
    [:div.sk-cube.sk-cube7]
    [:div.sk-cube.sk-cube8]
    [:div.sk-cube.sk-cube9]]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:title "Kaufmann -- CPA"]
   [:link {:rel         "stylesheet"
           :href        "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
           :integrity   "sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
           :crossorigin "anonymous"}]
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    [:script {:src         "https://code.jquery.com/jquery-3.3.1.slim.min.js"
              :integrity   "sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo"
              :crossorigin "anonymous"}]
    [:script  {:src         "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js"
               :integrity   "sha384-wHAiFfRlMFy6i5SRaxvfOCifBUQy1xHdJ/yoi7FRNXMRBu5WHdZYu1hA6ZOblgut"
               :crossorigin "anonymous"}]
    [:script {:src          "https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/js/bootstrap.min.js"
              :integrity    "sha384-B0UglyR+jN6CkvvICOB2joaf5I4l3gm9GU6Hc1og6Ls7i6U/mkkaduKaBhlAXv9k"
              :crossorigin  "anonymous"}]
    [:script {:src          "https://use.fontawesome.com/829a90d2af.js"}]
    [:script {:src          "https://maps.googleapis.com/maps/api/js?key=AIzaSyCM569Q3BkNg3L8ABoOqHD_bM5m_IEkCOg"}]
    (include-js "/js/app.js")
    (include-js "js/goog.js")]))

(defn index-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/items"
      ["" {:get {:handler index-handler}}]
      ["/:item-id" {:get {:handler index-handler
                          :parameters {:path {:item-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]]
    {:data {:middleware middleware}})
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))))
