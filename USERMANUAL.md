# OGAREdit User Manual

Martin Boerwinkle, martin@boerwi.net

## Running the Tool

This tool uses Java 17. You can get the Java 17 installer here: <https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.msi>.

Next, launch the tool by running `OGAREdit.bat` (or `OGAREdit.sh` for Linux users). This will open a black console (which you can ignore), and a small white window (which is the main OGAREdit window).

To start, press `File → New`, and use the file picker to create a new .ogr file. This type of file will store all of the resources used to design and curate a gallery (similar to an archive or database).

The left-pane should now present a tree of folders.

## The Resource Tree

The resource tree is in the left-pane of the main window. The root directory, and its immediate children ('Image', 'Text', 'Audio', etc.) cannot be modified. They serve as the backbone organization of the database.

The core interactions start by clicking to select. (Hold shift or ctrl while clicking to select multiple items at a time.) Then, right-click to open a menu. Depending on the situation, you might see the following menu options:

- Create : (When selecting a folder) Create an empty resource in the selected directory.
- Bulk Create : (When selecting a folder) Select multiple files from your filesystem to automatically create multiple resources in the selected folder.
- Create Folder : (When selecting a folder) Create a subfolder in the selected folder.
- Move Selected : Prompts to move the selected resources and folders to a different location. Note that you can only move non-top-level files and folders of the same type.
- Duplicate : (When selecting a single resource) Create a copy of the selected resource. The clone has the same dependencies as the original, but nothing depends on the clone, so you can safely edit the clone.
- Delete Selected : Delete the selected resources and folders.

When you have a single resource selected, the editor for that resource will appear in the right-pane.

### Renaming Items

You can rename non-root files and folders by triple-clicking on them. Once you are done, press enter to save the name.

## The Image Editor

The image editor is used for defining and cropping artworks. First, select an image from your filesystem with the 'Change Source' button.

- Dimensions : Set the default size of the artwork. Use 'Lock Aspect' to lock the proportions to those of the source image.
- Tags : Edit properties of the image by editing the visible text fields. Other properties are available by clicking 'Edit Extended Tags'. These tags can be used to generate descriptions for artworks.

The image can be rotated and cropped with the controls in bottom half of the editor. You can view your current work with 'Preview Crop'. If your image appears warped, try checking 'Lock Aspect'.

Images can be bulk-loaded (try right-clicking on an image folder) in two ways. When you start a bulk load, you will be presented with an option to load from JSON. If you press 'No', you can select images with the file picker.

If you press 'Yes', you select a single JSON file.
This is used to populate images and image tags automatically. The JSON must be structured like this:

	####
	{
		"imageName1":{
			"Filepath": "<ABSOLUTE_FILE_PATH_TO_IMAGE>",
			"Width": 0.53,
			"Height": 0.77,
			"Title": "Mona Lisa",
			"Artist": "Leonardo da Vinci",
			...
		},
		"imageName2":{
			...
		},
		...
	}
	####

"Width" and "Height" are in meters. "Filepath", "Width", and "Height" are required for each image. You can use other keys (like "Title") to populate tags.

## The Text Editor

The text editor is a standard text editor. Text resources are used for artwork labels.

You can harness the tags associated with images by enclosing tag names in angle brackets. For example, a text that included '\<artist\>' would be presented to gallery visitors as having the contents of the associated image's 'Artist' tag in its place.

You can organize your text using the following standard HTML tags:

- &lt;h1&gt; : Large header.
- &lt;h2&gt; : Medium header.
- &lt;small&gt; : Small text.
- &lt;u&gt; : Underline.
- &lt;b&gt; : Bold.
- &lt;i&gt; : Italic.

Every tag must be closed by a slash-variant of the tag. For example, bold text could appear as: &lt;b&gt;<b>Sample Bold Text</b>&lt;/b&gt;.

Underline, Bold, and Italic may be mixed with any tag. H1, H2, and Small are mutually-exclusive.

The classic-style museum label can be represented with this text:

	####
	<h1><b><title></b>  <i><date></i></h1>
	
	<artist>
	<medium>

	<description>
	####

## The Label Style Editor

The label style editor is used to describe how labels should appear in the gallery. The options available mirror those available through common word processors. Label styles are independent from galleries or texts because you may wish to reuse or mix-and-match styles between galleries and text templates.

## The Matte Style Editor

The matte style editor is used to describe how artworks should be matted (or framed) in the gallery. A matte style is a single edge color combined with up to three matte/frame layers.

Each matte layer goes 'up and out' from the previous layer, or starting at the edge of the artwork for the first layer. The 'top/bottom' and 'left/right' widths define how broad the matte layer is. The 'standoff' property describes the thickness of the matte. Mattes are commonly interior-beveled at 45 degrees. This can be accomplished by setting 'Bevel Angle' to 45 from the default of 90.

Finally, the overall 'Edge Color' is the color extending from the edge of the final matte back to the wall. This will often be best set to the same color as the outermost matte layer.

## The Audio Editor

The audio editor allows you to select a file from your filesystem using the 'Change Source' button (like the image editor).

You can enter a description for your own convenience, and you can press 'Play' to hear the audio.

OGAREdit accepts the common audio formats MP3 and WAV. 'Audacity' <https://www.audacityteam.org/download/windows/> is an open-source audio manipulation program; you can use it to record, clean, and trim audio, and export in the WAV format.

You should upload high-quality audio into OGAREdit. When a gallery containing audio is exported, the audio will automatically be converted to minimize the audio file sizes while preserving good speech fidelity.

## The Floorplan Editor

The floorplan editor is divided into three tabs: 'Properties', 'Walls', and 'Regions'. These tabs sit above a floorplan view which allows you to see the current state of the floorplan.

You can navigate the floorplan view by click and dragging to move and the mouse-wheel to zoom. You can also navigate with the arrow keys.

### Properties

The properties tab allows you to edit miscellaneous properties of the floorplan. Eye Height and Wall Height do not change how the floorplan appears in the editor, but they do change how the gallery is proportioned in the 3D view.

You can change the starting location and orientation of visitors using the 'Set Avatar Location' and 'Start Angle' controls. To change the start location of the avatar, press 'Set Avatar Location' and then click on the floorplan.

In a new floorplan, the avatar is positioned in the center of a square room and facing right.

### Walls

The walls tab is used to modify where walls exist in the gallery. Walls both constrain the users and serve as a location to hang artworks. There are three tool buttons available. To use the tools, click on the button and then click on the floorplan view.

#### Draw New Walls

To draw new walls, left-click to place an initial point. Continue left-clicking to create a sequence of connected walls. Finally, press 'Finish Draw' or right-click to stop drawing.

You can hold shift while you draw to constrain your walls to 45 degree increments.

#### Manual Wall Create

This textbox can be used to insert walls directly by typing coordinates. First, select the textbox. Then, type four numbers (representing coordinates in meters, separated by commas, spaces, or semicolons). Finally, press `enter` to insert the wall. `Enter` will also clear the textbox so that you can immediately enter another wall.

This is an example of a valid horizontal line from (0,1) to (0, 3):

	####
	0 1.0, 0.00, 3
	####

#### Move Corners

To move corners, click and drag on a wall corner or end. You can view the current position of a corner or end by hovering over it.

#### Delete Walls

To delete walls, hover over a wall until it turns red (this should be instant, once your cursor gets close to the wall). Click to delete the red-highlighted wall.

#### Thick Walls

The 'Thick Walls' checkbox toggles walls having one or two sides. It is recommended that you leave 
'Thick Walls' enabled unless you need precise control over wall placement.

### Regions

The regions tab is used to modify the highlighted regions. A new floorplan has a small square region inset inside of a square room. Regions do not appear to users, but they may be useful for data analysis.

The region tab has a dropdown box which begins with a single region defined. All of the tools on the region tab operate on the currently selected region.

'New Region' and 'Delete Region' are used to change which regions appear in the dropdown menu.

'Add/Move/Delete Points' operate on the points of the selected region. You must create at least three points before a region will become visible.

## The Gallery Editor

The gallery editor is used to curate resources in a floorplan. To begin on a new gallery, first click 'Set Floorplan' and choose a floorplan you have created.

The colors of the wall, floor, and ceiling can be set with the colored buttons in the 'Colors' box. Colors are specified as hex color codes (as in <https://en.wikipedia.org/wiki/Web_colors>).

Once a floorplan is imported, you can click on a wall to curate the art on that wall. The small red line in the middle of the selected wall must point toward the viewer. For example, in a square room, all indicator lines should point 'inward'. Clicking on a wall opens a 'Curation Popup'.

Users can be permitted to click on artworks to view them in fullscreen by checking 'Click-to-Examine'. Text labels are only accessible in galleries with this option enabled, since they appear in the fullscreen examination view mode.

If 'Click-to-Examine' is enabled, 'Autoplay Audio' becomes an option. If this option is checked, then any associated audio will begin playing as soon as participants examine an artwork.

If 'Click-to-Examine' is not enabled, but audio is assocated, then audio will begin playing if a participant clicks on the artwork. The participant will continue to be able to move around, but the audio will stop if the participant's avatar travels further than click-distance away from the art.

The resolution of images exported in the gallery can be changed with the 'px/mm Image Resolution' number selector. 1.2 px/mm is a reasonable value. 2 px/mm is roughly 50 DPI. You can convert from DPI to px/mm with `"px/mm" = DPI/25.4`.

### The Curation Popup

Curation popups present a side-view of a wall in the gallery. Press 'Add Artworks' to add art to the wall. Click and drag the art to move it on the wall, or edit the 'Center' coordinates after clicking to select an art piece. The currently selected art piece is denoted by a red dot in the center of the art.

To associate text with an artwork, click the 'Associate Text' button. You will be prompted to select a text resource, and then a label style resource.

To associate audio with an artwork, click the 'Associate Audio' button. You will be prompted to select an audio resource.

## Exporting

To export a gallery, select a single gallery resource and use the 'Project → OGAR Gallery Export' top-menu option. This will export a zip file which can be used to host the gallery online.

## Limits

For maximum technological compatibility, it is recommended that galleries not exceed the following parameters:

### Wall Segments

Wall drawing and collisions have minimal performance impact, so even thousands of walls should not detract from performance.

### Artworks

Internet speeds are the primary limiting factor on the number of artworks in a gallery. Internet speeds vary by orders of magnitude, even within the US. At the time of writing, AT&T offers plans ranging between 0.8 MBPS to 1 GBPS.  Artworks in OGAR are approximately 500KB each, which means a single artwork may take anywhere from 5 milliseconds to over five seconds to load. Account for shared internet and unreliable wifi, and the situation is even worse.

As a rule of thumb, estimate art loading will take 2 seconds per image. It may be possible to pre-download images while users complete other tasks, such as surveys, by pre-fetching them.

It is not recommended to exceed 256 images in a single gallery.

### Client Hardware Recommendations

Clients should have:

- An updated version of Chrome, Firefox, or Microsoft Edge
- A desktop or laptop produced less than 8 years ago
- A mouse or trackball (touchpads may be configured to disable while the keyboard is in use)
- WebGL-compatible 3D acceleration

## Crashes

You may get a popup which informs you that 'OGAREdit has crashed'. This is typically not a problem, and should not prevent you from saving your work. It will inform you that it wrote a crash log. Please consider reviewing and sending the crash log to martin@boerwi.net along with a description of what happened before the crash.

## Notes

### Reference Editing Considerations

Galleries use floorplans, images, text, and audio by reference. This means that editing resources may alter other resources with depend on them. Consider creating separate databases for each project or duplicating any resources which you may wish to change.

This is particularly dangerous when editing floorplans. If you delete a wall on a referenced floorplan, all galleries which use that floorplan will lose all art placed on the deleted wall.

### Wall T-Intersection Considerations

If you have a wall which T-intersects into a different wall, you may want to consider splitting the T'd wall into two walls to create a 3-way intersection. This will allow for a better curation experience, since a single curation screen won't include a hidden obstruction in the middle.

