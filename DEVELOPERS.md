# To Developers:

This document is designed to serve as an introduction to the design and layout of this software for anyone who may seek to build, modify, or maintain it.

## Layout

- `meson.build` : See 'Building', below.
- `external/` : This contains external resources. You will probably have to hunt these down yourself. The build script expects the files outlined in `external/EXTERNAL.md` to exist in this directory.
- `OGAREdit.bat/.sh` : Launcher scripts. You must use the versions copied into the build directory by meson (see "Building", below)
- `net/` : Source directory.

## Building

To build this package, you'll need:

- a Java 17-compatible JDK
- `meson`
- `bash`
- `cairosvg`
- `markdown`
- to fetch the files outlined in `external/EXTERNAL.md`

Debian/Ubuntu users may use apt to install the following packages: `meson` `bash` `openjdk-17-jdk` `cairosvg` `markdown`.

Steps:

1. `meson build`
2. `cd build`
3. `meson compile` (this also generates documentation at build/javadoc/index.html)

## Packages

This software is composed of one main package and a few utility packages.

### `net.boerwi.convexhull`

This package finds a convex hull from a set of points. It is used for determining floorplan regions. The meat of this package comes from `GRAHAM-SCAN` in Introduction to Algorithms, 3rd, CHP 33.

### `net.boerwi.linemiter`

This package converts a collection of line segments into a widened and mitered collection of line segments. This is used for automatically thickening walls of floorplans.

### `net.boerwi.ogaredit`

This is the main package. All of the database, UI, IO, etc. is performed in this package.

### `net.boerwi.snowfluke`

This is a UID generator package based on Snowflake IDs (<https://en.wikipedia.org/wiki/Snowflake_ID>), but tuned for the needs of this software. These IDs are used to avoid collisions for user-created artifacts, such as resources or data blobs.

### `net.boerwi.extrawidgets`

A collection of custom interfaces designed for this project.

## Special Topics

### Adding a new resource type

- Add to OE\_DB.java upgradeDBVersion function (add to oetype, and increment oeversion)
- Multiple other things need to be added to OE\_DB.java
- Add to resource/dbDef.sql
- Add to OE\_ResType
- Add to ResEditor
- Make new classes. For example, for MatteStyle, I add OE\_MatteStyle.java and MatteStyleEditor.java
- Add new java files to meson.build in java\_files array
- If you want to support bulk loading, look at OE\_Resource.java-\>createFromData()
- Add loader reference to switch in OE\_Resource.java-\>loadFromJson()
- Update USERMANUAL.md
