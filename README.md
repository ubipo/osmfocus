**Fork Notice**

This app is a complete re-write of the now (07-11-2020) defunct OSMfocus by Network42 / MichaelVL.  
Google Play: https://play.google.com/store/apps/details?id=dk.network42.osmfocus
GitHub: https://github.com/MichaelVL/osm-focus

# OSMfocus Reborn

[![F-Droid](https://img.shields.io/f-droid/v/net.pfiers.osmfocus)](https://f-droid.org/en/packages/net.pfiers.osmfocus/) [![GitHub release (latest by date)](https://img.shields.io/github/v/release/ubipo/osmfocus)](https://github.com/ubipo/osmfocus/releases/latest) <a href="https://www.buymeacoffee.com/pfiers" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" height="20"></a>

OSMfocus Reborn is an open source tool for examining OpenStreetMap elements by moving around on a
map.  
Written in Kotlin using Android Jetpack.

![Feature Graphic](images/featuregfx.png)

Move the crosshair in the middle of the map over a building or road to view its keys and values. A
line will be drawn connecting the element with a box on the side of the screen. This box contains
every tag of the element in OpenStreetMap. Use this information to find bugs or to investigate an
area closer. Click on one of the boxes if you want even more detailed information.

Change the basemap (background layer) or add your own by going to the settings screen (cog icon).

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="75">](https://f-droid.org/packages/net.pfiers.osmfocus) [<img height="75" alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png">](https://play.google.com/store/apps/details?id=net.pfiers.osmfocus)

## Source, issue tracking and more info

Visit [https://github.com/ubipo/osmfocus](https://github.com/ubipo/osmfocus)

Other stuff by me: [https://pfiers.net](https://pfiers.net)

## Development

### Architecture

OSMfocus Reborn is written in Kotlin using Android Jetpack. It does not use ViewModels, but
instead  
keeps UI state in the relevant composables ([`view`](app/src/main/java/net/pfiers/osmfocus/view))
and model state in the relevant
repositories ([`service`](app/src/main/java/net/pfiers/osmfocus/service)). The entrypoint activity
is [`view.MainActivity`](app/src/main/java/net/pfiers/osmfocus/view/MainActivity.kt), which uses the
compose [`setContent`](https://developer.android.com/jetpack/compose/interop/interop-apis#compose-in-views)
method to show either the [`MapView`](app/src/main/java/net/pfiers/osmfocus/view/map/MapView.kt)
or [`Settings`](app/src/main/java/net/pfiers/osmfocus/view/settings/Settings.kt) page. The `MapView`
is the core UI component of the app, responsible for showing the slippy map (i.e. interactive world
map), the tag boxes (boxes with colored borders show element details), and the map overlays (for
elements and for the connecting line from tag box to element). Downloading, throttling, and state
management for map elements is handled by
the [`ElementsRepository`](app/src/main/java/net/pfiers/osmfocus/service/osmapi/ElementsRepository.kt)
, which calls into the relevant methods of
the [OpenStreetMap API](https://wiki.openstreetmap.org/wiki/API_v0.6) ([`service.osmapi`](app/src/main/java/net/pfiers/osmfocus/service/osmapi)).

## Notices

OSMfocus Reborn allows you to view OpenStreetMap data. This data
is [© (Copyright) OpenStreetMap contributors](https://www.openstreetmap.org/copyright) and is
available under the [Open Database License](https://opendatacommons.org/licenses/odbl/).

OSMfocus Reborn uses (remixed) names and ideas from OSMfocus (MichaelVL) v0.1.1r1, which is
available under the [Apache License 2.0](https://github.com/MichaelVL/osm-focus/blob/master/LICENSE)
.

OSMfocus Reborn uses a remixed version of the OpenStreetMap logo, as per
the [Trademark Policy §3.5](https://wiki.osmfoundation.org/wiki/Trademark_Policy).

OSMfocus Reborn allows you to view map tiles from (but does not rely on) third-party and potentially
non-free services.
