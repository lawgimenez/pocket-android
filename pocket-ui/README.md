# Pocket UI Components

This module contains reusable UI and UX building blocks for Pocket apps that follow Pocket's design guidelines.

## Naming Conventions

### Java

In most cases, can just name the class normally, no need for a Pocket or Pkt prefix, since the java namespaces provide that specific info.


### XML Resources

Colors begin with `pkt_`

Colors that are state selectors that require being loaded with [NestedColorStateList](src/main/java/com/pocket/ui/util/NestedColorStateList.java) begin with `pkt_nst_`.

Drawables that are icons begin with  `ic_pkt_`

Drawables that are color state selectors as drawables begin with `cl_pkt_`

Other resources that don't have a special prefix defined start with `pkt_`

## Colors

All colors should be defined in colors.xml, see notes in that file for details.

### Nested State Lists

To help with code reuse, some color and drawable state selectors reference other state selectors.
However, for ColorStateLists, Android does not support this out of the box, trying to reference another
ColorStateList just extracts the default color from that state list.

To enable ColorStateLists to reference other ColorStateLists in xml, load your ColorStateList with [NestedColorStateList](src/main/java/com/pocket/ui/util/NestedColorStateList.java):

`NestedColorStateList.get(context, R.color.pkt_nst_icon_button_coral)`

There are also some xml attributes that support nested color state lists in layouts:

* `app:drawableColor` in ThemedImageView 
* `app:compatTextColor` in ThemedTextView 

To aid in making sure it is clear when a color state list requires being loaded in this way,
those color files should be prefixed with `pkt_nst_`, otherwise loading them normally will have
the wrong colors (it won't crash, it just won't have the right colors).

Android does support this for state list drawables! So drawables can naturally nest other color state drawables.
Just make sure you reference another color state *drawable*, not color state list.


## App Bar

A Themed ViewGroup with:
* Height of @dimen/pkt_app_bar_height
* A [Thin Divider](#dividers) along the content edge
* @drawable/pkt_bg_app_bar background as the background
* Vertically center content
* Left and right margins should visually be @dimen/pkt_side_grid.

### App Bar Icons

Use `IconButton` for each app bar icons.

When an IconButton is used as the far left or right element in an app bar, add the following to it in the xml layout:

```
android:layout_width="wrap_content"
android:paddingLeft="@dimen/pkt_side_grid"
android:paddingRight="@dimen/pkt_side_grid"
```

That will make the icon visually align with the side grid lines


## Box Buttons

There are six styles of boxy buttons:

Default button:
```xml
<com.pocket.ui.view.button.Button
		android:layout_width="wrap_content"
		android:layout_height="wrap_content"
		android:text="@string/your_label" />    
```

Button styled for error states:
```xml
<com.pocket.ui.view.button.ErrorButton
		android:layout_width="wrap_content"
		android:layout_height="wrap_content"
		android:text="@string/your_label" />    
```

Button styled for use on a colored area:
```xml
<com.pocket.ui.view.button.OnColorButon
		android:layout_width="wrap_content"
		android:layout_height="wrap_content"
		android:text="@string/your_label" />    
```

A button styled for a submit button at the bottom of a screen: 
```xml
<com.pocket.ui.view.button.SubmitButton
		android:layout_width="match_parent"
		android:layout_height="wrap_content"
		android:text="@string/your_label" />    
```

TODO login screens have another variant as well


## Icon Buttons

Icon buttons follow a pattern like:

```xml
<com.pocket.ui.view.button.IconButton
		android:id="@+id/app_bar_archive"
		style="@style/ArchiveButton" />    
```

Reuse or create a style for each new icon type, specific styles look like:

```xml
<style name="ArchiveButton" parent="IconButton">
    <item name="android:contentDescription">@string/lb_tooltip_archive</item>
    <item name="srcCompat">@drawable/ic_pkt_archive_checked</item>
    <item name="tooltip">@string/lb_tooltip_archive</item>
</style>
```

See the project-tools vector importer tool to help import vector files and selectors.

Note: IconButtons are checkable, but not by default. Enable checkable states with the following in your style:
```xml
<item name="checkable">true</item>
```

## Dividers

Some horizontal divider variants:
```xml
<com.pocket.ui.view.themed.ThemedView style="@style/ThinDivider" />
```
```xml
<com.pocket.ui.view.themed.ThemedView style="@style/ThickDivider" />
```

## Fonts

Pocket's interface font is Graphix. From xml, it can be used like so:

```xml
<com.pocket.ui.view.themed.ThemedTextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:typeface="graphik_lcg_bold" />
```

From code, use `Fonts.get(Context, Font)`.

In a WebView, see the implementation notes in the Fonts class docs for details on how to use.

## Bottom Sheets

Standard Design Support Library (aka. Material Components Library) implementation of a bottom sheet
with some standard styling:
* Add a CoordinatorLayout laid out over the are where the bottom sheet can expand.
* Add a themed ViewGroup as CoordinatorLayout's child with `app:layout_behavior="com.google.android.material.bottomsheet.BottomSheetBehavior"`.
* For standard background on that ViewGroup use `android:background="@drawable/bg_pkt_bottom_sheet"`.
* To add a standard "drag handle" at the top use `com.pocket.ui.view.BottomSheetDragHandle`
  (usually with 30dp spacing both above and below the handle).
  Set width and height to `wrap_content` to get the standard size.
* Add your specific content below.

















Work in Progress, ignore:



## Snackbar Dialogs

These are small popup or messaging bubbles that can appear at the bottom or top of a screen, or within a collection.

Use [SnackbarMessage](src/main/java/com/pocket/ui/view/notification/PktSnackbar.java)'s builder to create and show them.

Snackbar (small rounded dialog)
Snackbar - Red Error
Snackbar - Green Actionable
Snackbar - Green Large

## Item's

To display an Item, use a [ItemRowView](src/main/java/com/pocket/ui/view/item/ItemRowView.kt).

Item Image
Item Row/Tile attribution label

## Badges

Items, tags and other elements sometimes use Badges. To display one, use [BadgeView](src/main/java/com/pocket/ui/view/badge/BadgeView.kt).
Use a [BadgeLayout](src/main/java/com/pocket/ui/view/badge/BadgeLayout.kt) to display a row/list of badges.




Small Profile Label
Item Actions
Item Actions Reveal UX
Item Tile
Spoc Row
Spoc Tile
Rec Tile
Save Button
Social Actions
Social Actions - Repost
Social Actions - Like
Button - Blue / White Text
Button -  Blue Checkable
Button  - Red / White Text
Button - White / Green Text
List
Grid
Staggered Grid
Notification (Activity) Row
Thin Divider
Thick Divider
Fake Empty Item Row
Empty / Error State
Text Button
Small Text Button
Large Profile
Icon And Text Button
Search Field
Section Header
Text Row
Profile Row
Circle Checkbox
Switch
Save Extension
Tag Text Box
Side by Side Form Field
Setting
Comment Field
Selectable Option Row
Submit  button
Bottom Sheet
Large Circle Button
Bottom Sheet Row
Display Settings
Reader Attribution
Splash
Top Labeled Form Field
Small/Disclaimer  Text
Wide Button
Onboarding / Intro Module
Carousel dots
Highlight
Highlight Row



