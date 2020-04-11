(ns netlogger.core
    (:require [reagent.core     :as reagent :refer [adapt-react-class atom ]]
              [reagent.dom      :as dom     :refer [dom-node render]]
              [reagent.session  :as session]
;;              [cljsjs.react-bootstrap :refer [Nav]]
;;              [cljs.core.async              :refer [<!]]a
;;              [cljs-http.client :as http]
              [reitit.frontend  :as reitit]
              [clerk.core       :as clerk]
              [accountant.core  :as accountant]))

;; -------------------------
;; Routes

(def colheaders [{ :label "Call"      :widthclass "col-md-1 sm-hidden" },
                 { :label "First",    :widthclass "col-md-1 col-sm-6" },
                 { :label "Last",     :widthclass "col-md-1 col-sm-6" },
                 { :label "City",     :widthclass "col-md-1 col-sm-6" },
                 { :label "State",    :widthclass "col-md-2 col-sm-6" },
                 { :label "Class",    :widthclass "col-md-1 col-sm-6" },
                 { :label "Prev",     :widthclass "col-md-1 col-sm-6" },
                 { :label "In",       :widthclass "col-md-1 col-sm-6" }])

(def statelist [ "--Choose--", "AB", "AE", "AK", "AL", "AR", "AS", "AZ", "BC", "CA", "CO", "CT", "DC", "DE", "FL", "FM", "GA", "GU", "HI", "IA", "ID", "IL",
                "IN", "KS", "KY", "LA", "MA", "MB", "MD", "ME", "MH", "MI", "MN", "MO", "MP", "MS", "MT", "NB", "NC", "ND", "NE", "NH", "NJ",
                "NL", "NM", "NS", "NT", "NU", "NV", "NY", "OH", "OK", "ON", "OR", "PA", "PE", "PR", "QC", "RI", "SC", "SD", "SK", "TN",
                "TX", "UT", "VA", "VI", "VT", "WA", "WI", "WV", "WY", "YT" ])

(def newcallsign (reagent/atom ""))

(def logentries (reagent/atom [{ :call "wd5etd" :first "stu" :last "lefkowitz" :address "123 nowhere st"  :city "outthere" :state "OK" :zip "12345" :license "general"
                                 :lastcheckin "04/01" :checkintime "8:30"},
                               { :call "n5xqk"  :first "johnny" :last "renfro" :address "321 anywhere rd" :city "bville"   :state "OK" :zip "73111" :license "extra"
                                :lastcheckin  "never" :checkintime "8:32"}]))

(def router
  (reitit/router
   [["/"         :index]
    ;;    ["/gmap"      :gmap]
    ["/log"      :log]
    ["/contact"  :contact]
    ["/about"    :about]
    ["/login"    :login]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;; macro
;;(defmacro def-reagent-class [var-name js-ns js-name]
;;  `(def ~var-name
;;     (reagent/adapt-react-class
;;      (aget ~js-ns ~js-name))))

;; (def-reagent-class "Button" js/ReactBootstrap "Button")

;; (def Button (reagent/adapt-react-class (aget js/ReactBootstrap "Button")))

(path-for :index)

(defn gmap-render []
  [:div {:style {:height "400px" :width "400px"}}])

(defn gmap-did-mount [this]
  (let [map-canvas  (dom/dom-node this)
         map-center  (js/google.maps.LatLng. 36.085000 -95.923500)
        map-options (clj->js {"center" map-center
                              "zoom"   15})
        my-map      (js/google.maps.Map. map-canvas map-options)]
    (js/google.maps.Marker. (clj->js  {"position" map-center
                                       "label"    "Center of Universexs."
                                       "animation" js/google.maps.Animation.DROP.
                                       "map"      my-map }))))

(defn gmap []
  (reagent/create-class {:reagent-render gmap-render
                         :component-did-mount gmap-did-mount}))

;; -------------------------
;; Page components

(defn navitem [id mypath label & [icon]]
      [:li.link-wrapper>a.nav-link
       {:href mypath :id id
        ;; :on-click #(.click (.getElementById js/document "toggler"))
        }
        (if icon
          [icon {:aria-hidden "false"}])
        (str " " label) [:span {:class "sr-on"}]])

(def navbar
  [:nav.navbar.navbar-expand-sm.navbar-light.bg-light
   [:a { :href "/" :class "navbar-brand" } "Ham Net Logger"]
   [:ul { :class "navbar-nav mr-auto" }
    (navitem "home"     (path-for :index)
             "Home"     :i.fa.fa-home)
   ;;     (navitem "map"    (path-for :gmap)
    ;;              "Map"    :i.fa.fa-lightning)
    (navitem "log"      (path-for :log)
             "Log"      :i.fas.fa-pen)
    (navitem "contact"  (path-for :contact)
             "Contact"  :i.fa.fa-phone)
    (navitem "about"    (path-for :about)
             "About"    :i.fa.fa-info)]
    [:a { :class "btn btn-outline-success" :role "button" :href "/login" } "Login"]])

(defn home-page []
  (fn []
    [:div.main
     [:h3 "Howdy"]
     [:button {:type "button" :class "btn btn-primary" } "with a button"]]))

(defn appendcallandclear []
  (swap! logentries conj { :call @newcallsign :first "" :last "" :address "" :city "" :state "" :zip "" :lastcheckin "never" :checkintime (.getTime (js/Date.)) })
  (reset! newcallsign ""))

(defn log-submit [e]
  (.preventDefault e)
  (if (and @newcallsign (re-matches #"[aknv][a-z]{0,1}[0-9][a-z]{1,3}" @newcallsign))
    (appendcallandclear)
    (js/alert "Enter a valid call sign")))

(defn state-chooser [thekey chosenone]
  [:select { :key thekey :value chosenone :style { :font-size "small" } :disabled true }
   (for [st statelist]
     [:option { :value st :key st } st ])])

(defn text-in-col [call itsname value]
  [:td.col-md-1.col-sm-6  { :key (str itsname "-" call) } value])

(defn log-header []
  [:tr.row.ml-1 { :key "header" :style { :padding-left "10px" :font-size "medium"
                                        :background-color "blue" :color "white" } }
   (for [header colheaders]
     [:th { :key (header :label) :class (header :widthclass) } (header :label)  ])])

(defn log-entry [log]
  [:tr.row.ml-1 { :key (log :call) :style { :font-size "medium" } }
   [:td.col-md-1.col-sm-6  { :key (str "call-"    (log :call)) } (log :call)]
   (text-in-col (log :call) "first"   (log :first))
   (text-in-col (log :call) "last"    (log :last))
   (text-in-col (log :call) "cityt"   (log :city))
   [:td.col-md-2.col-sm-6  { :key (str "state-"   (log :call)) }
    (state-chooser (str "st-chooser-" (log :state)) (log :state))]
   (text-in-col (log :call) "lic"     (log :license))
   (text-in-col (log :call) "lastdt"  (log :lastcheckin))
   (text-in-col (log :call) "checkin" (log :checkintime))])

(defn log-page []
  (fn []
    [:div.main
     [:div.mb-1 { :style { :width "100%" :float "left" } }
      [:form { :on-submit log-submit }
       [:input.form-control { :type "text" :id "callsign"    :aria-describedby "callsignsearch"
                             :placeholder "Enter Call Sign"  :value @newcallsign
                             :on-change #(reset! newcallsign (-> % .-target .-value))
                             :style { :float "left" :width "20%" } } ]
       [:button.btn.btn-primary { :type "submit" :style { :margin-left "5px" :margin-bottom "5px" :float "left"} } "Add" ]]]
     [:div.table-responsive
      [:table.table.table-striped { :font-size "small" }
       [:tbody
        (log-header)
        (for [log @logentries]
          (log-entry log))]]]]))

(defn contact-page []
  (fn []
    [:div.main
     [:h3 "Contact"]
     [:div.floater
      [:p.contact "Unknown"
       [:br]
       "TULSA, OK 74135"
       [:br]]]]))

(defn about-page []
  (fn []
    [:span.main
     [:h3 "About"]
     [:p "This is the about page"]]))

(defn login-page []
  (fn []
    [:div.main
     [:h3 "Login"]
     [:form
      [:div {:class "form-group"}
       [:label {:for "login"} "User Name"]
       [:input {:type "text" :id "login" :class "form-control" :aria-describedby "loginhelp" :placeholder "Enter login"} ]
       [:small {:id "loginhelp" :class "form-text text-muted" } "We will never share your email address with anyone" ]]
      [:div { :class "form-group" }
       [:label {:for "password" } "Password" ]
       [:input.form-control {:type "password" :id "password" :placeholder "Password" }]
       [:small {:id "loginhelp" :class "form-text text-muted" } "Eight characters minimum" ]]
      [:button { :type "submit"  :class "btn btn-primary" } "Submit" ]]]))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index    #'home-page
    ;;    :gmap     #'gmap
    :log      #'log-page
    :about    #'about-page
    :contact  #'contact-page
    :login    #'login-page))

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
        [:p "©2020 Unknown."]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (dom/render [current-page]
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
            ;;            pathname     (second (re-matches #"^.(.*)" path))
            ]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists? (fn [path]
                    (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))
