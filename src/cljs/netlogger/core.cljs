(ns netlogger.core
    (:require [reagent.core     :as reagent :refer [adapt-react-class atom ]]
              [reagent.session  :as session]
              [cljsjs.react-bootstrap]
              [cljs.core.async              :refer [<!]]
              [cljs-http.client :as http]
              [reitit.frontend  :as reitit]
              [clerk.core       :as clerk]
              [accountant.core  :as accountant]))

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/"         :index]
;;    ["/gmap"      :gmap]
    ["/services" :services]
    ["/contact"  :contact]
    ["/about"    :about]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; macro
(defmacro def-reagent-class [var-name js-ns js-name]
  `(def ~var-name
     (reagent/adapt-react-class
      (aget ~js-ns ~js-name))))

;; (path-for :index)

(defn gmap-render []
  [:div {:style {:height "400px" :width "400px"}}])

(defn gmap-did-mount [this]
  (let [map-canvas  (reagent/dom-node this)
        map-center  (js/google.maps.LatLng. 36.085000 -95.923500)
        map-options (clj->js {"center" map-center
                              "zoom"   15})
        my-map      (js/google.maps.Map. map-canvas map-options)]
    (js/google.maps.Marker. (clj->js  {"position" map-center
                                       "label"    "Rodney D. Kaufmann CPA, Inc."
                                       "animation" js/google.maps.Animation.DROP.
                                       "map"      my-map }))))

(defn gmap []
  (reagent/create-class {:reagent-render gmap-render
                         :component-did-mount gmap-did-mount}))

;; -------------------------
;; Page components

(defn navitem [id mypath label & [icon]]
      [:li.link-wrapper>a.nav-link
        {:href mypath :id id :on-click #(.click (.getElementById js/document "toggler"))}
        (if icon
          [icon {:aria-hidden "false"}])
        (str " " label) [:span {:class "sr-on"}]])

(def navbar
  [:nav.navbar.navbar-expand-lg.navbar-light.bg-light
   [:a { :href "/" } "Rodney D. Kaufmann, CPA, Inc."]
   [:button.navbar-toggler { :id "toggler"  :type "button"
                            :data-toggle "collapse"
                            :data-target "#navbarSupportedContent"
                            :aria-controls "navbarSupportedContent" :aria-expanded "false"
                            :aria-label "Toggle navigation"}
    [:span.navbar-toggler-icon]]
   [:div.collapse.navbar-collapse {:id "navbarSupportedContent"}
    [:ul.navbar-nav.mr-auto
     (navitem "home" (path-for :index)
              "Home"     :i.fa.fa-home)
;;     (navitem "map" (path-for :gmap)
;;              "Map"     :i.fa.fa-lightning)
     (navitem "services" (path-for  :services)
              "Services" :i.fa.fa-wrench)
     (navitem "contact"  (path-for :contact)
              "Contact" :i.fa.fa-phone)
     (navitem "about"    (path-for :about)
              "About" :i.fa.fa-info)]]])

(defn home-page []
  (fn []
    [:span.main
     [:p.home "Only choose the best when it comes to your money!"]
     [:p.home2 "For over 28 year
                and businesses "]
     [:p.home2 "We have moved.  See our "
               [:a {:href (path-for :contact)} "contact"]
               " page for our location and directions."]]))

(defn contact-page []
  (fn []
    [:div.main
     [:h3 "Contact"]
     [:div.floater
      [:p.contact "RODNEY D. KAUFMANN CPA, INC."
       [:br]
       "5416 S YALE AVE"
       [:br]
       "SUITE 650"
       [:br]
       "TULSA, OK 74135"
       [:br]
       "We are located just South of I-44 on Yale Ave"
       [:br]  " on the West Side of the road."]
      [:p "918-747-7433"
       [:br]
       "918-747-7488 (Fax)"]
      [:a {:href "mailto:rod@netloggercpa.com"} "rod@netloggercpa.com"]
      [:p "M-F : 8am to 5pm"]]
     [:iframe { :src "https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d1585.0635004080923!2d-95.92449889772709!3d36.08499428596624!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x87b6ed34b9edb875%3A0x8abbdf0c88858e52!2sRodney+D+Kaufmann+Inc!5e1!3m2!1sen!2sus!4v1548798011634"
               :width "500px" :height "500px" :frameBorder 0 :allowFullScreen true :style {:margin 0}}]]))

(defn about-page []
  (fn []
    [:span.main
     [:h3 "About"]
     [:p "Rodney D. Kaufmann founded Rodney D. Kaufmann CPA in 1987. Rod holds a Bachelor of Science in Business Administration from Kansas State University receiving his CPA in 1982. He is a Certified Public Accountant in Oklahoma and Kansas. Rod’s professional expertise covers a wide range of industries including wholesale, retail, manufacturing, commercial and multi housing real estate, oil and gas, service companies, non-profits and foundations along with estate and tax planning. Rod is a member of the AICPA, CGMA and OSCPA."]
     [:p
      [:img {:src "images/aicpafront.png" :className "proimage"}]
      [:img {:src "images/oscpafront.png" :className "proimage"}]]]))

(defn services-page []
  (fn []
    [:div.main
     [:h2 "Services"]
     [:p "Rodney D. Kaufmann CPA provides a wide range of services to individuals and businesses in a variety of industries. With over 28 years of experience, we provide personalized service to meet each client’s specific needs in planning for the future and achieving their goals in an ever-changing financial and regulatory environment."]

     [:ul
      [:li "Individual Tax Returns"]
      [:li "Corporate Tax Returns"]
      [:li "Partnership Tax Returns"]
      [:li "Not for Profit Tax Returns"]
      [:li "Tax Planning & Preparation"]
      [:li "IRS Representation"]
      [:li "Financial Statement Preparation"]
      [:li "Accounting Review"]
      [:li "Tax Audit Representation"]
      [:li "Estate & Trust Tax Preparation"]
      [:li "Sales Tax Compliance"]
      [:li "Reviews & Compilations"]
      [:li "Bookkeeping & Payroll"]
      [:li "Other Services"]]]))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index    #'home-page
;;    :gmap     #'gmap
    :services #'services-page
    :about    #'about-page
    :contact  #'contact-page))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div.container-fluid
       [:header.pad-head
        navbar]
       [page]
       [:footer.footer
        [:p "©2019 Rodney D. Kaufmann CPA, Inc."
         ]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page]
                  (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
;;      (println path)

      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)
            pathname     (second (re-matches #"^.(.*)" path))]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
