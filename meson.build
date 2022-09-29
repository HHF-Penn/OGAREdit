# Project definition
project('OGAREdit', 'java')

# Find all the programs we need for a build
c_markdown = find_program('markdown')
c_jar = find_program('jar')
c_javadoc = find_program('javadoc')
c_cairosvg = find_program('cairosvg')

# All Java files for this project
java_files = [
	'net/boerwi/ogaredit/package-info.java',
	'net/boerwi/ogaredit/Main.java',
	'net/boerwi/ogaredit/OE_ViewControl.java',
	'net/boerwi/ogaredit/OE_ViewControl_Swing.java',
	'net/boerwi/ogaredit/OE_DB.java',
	'net/boerwi/ogaredit/OE_ResType.java',
	'net/boerwi/ogaredit/OE_Resource.java',
	'net/boerwi/ogaredit/OE_Image.java',
	'net/boerwi/ogaredit/OE_Text.java',
	'net/boerwi/ogaredit/OE_LabelStyle.java',
	'net/boerwi/ogaredit/OE_MatteStyle.java',
	'net/boerwi/ogaredit/OE_Audio.java',
	'net/boerwi/ogaredit/OE_Floorplan.java',
	'net/boerwi/ogaredit/OE_Gallery.java',
	'net/boerwi/ogaredit/TextureAtlas.java',
	'net/boerwi/ogaredit/GridView.java',
	'net/boerwi/ogaredit/ResEditor.java',
	'net/boerwi/ogaredit/ImageEditor.java',
	'net/boerwi/ogaredit/TextEditor.java',
	'net/boerwi/ogaredit/LabelStyleEditor.java',
	'net/boerwi/ogaredit/MatteStyleEditor.java',
	'net/boerwi/ogaredit/AudioEditor.java',
	'net/boerwi/ogaredit/FloorplanEditor.java',
	'net/boerwi/ogaredit/GalleryEditor.java',
	'net/boerwi/ogaredit/RadioJButtonMgr.java',
	'net/boerwi/ogaredit/OE_BlobMgr.java',
	'net/boerwi/ogaredit/OE_Blob.java',
	'net/boerwi/ogaredit/AsyncBlobServer.java',
	'net/boerwi/ogaredit/AsyncBlobWaiter.java',
	'net/boerwi/ogaredit/OE_DBBlobMgr.java',
	'net/boerwi/ogaredit/OE_Color.java',
	'net/boerwi/ogaredit/OE_ResourceSelector.java',
	'net/boerwi/ogaredit/CSVUtils.java',
	'net/boerwi/convexhull/package-info.java',
	'net/boerwi/convexhull/ConvexHull.java',
	'net/boerwi/convexhull/CHPoint.java',
	'net/boerwi/snowfluke/package-info.java',
	'net/boerwi/snowfluke/Snowfluke.java',
	'net/boerwi/linemiter/package-info.java',
	'net/boerwi/linemiter/LineMiter.java',
	'net/boerwi/extrawidgets/package-info.java',
	'net/boerwi/extrawidgets/HelpIcon.java',
]

# Classpath info for building. This doesn't need to include Java SPI provider jars
cp_jars = [
	'json.jar',
	'sqlite-jdbc.jar',
	'commons-imaging.jar',
	'jump3r.jar',
]

classPathSep = ':' # ':' for *nix
classpath_string = classPathSep.join(cp_jars)

# Basic java compilation into a jar ("Intermediate Jar")
res_inter_jar = jar('OGAREditNoRes',
	java_files,
	java_args : [
		'-cp', classpath_string,
		'-Xlint:all,-auxiliaryclass',
		'-encoding', 'UTF-8',
		'-source', '17', '-target', '17'
	],
)

subdir('resource')

# We need to modify the jar to add resources (like icons, etc)
res_jar = custom_target(
 'OGAREdit',
 build_by_default : true,
 input : res_inter_jar,
 depend_files : copy_res,
 depends : built_res,
 output : 'OGAREdit.jar',
 feed : true,
 capture : true,
 command : [c_jar, '--update', copy_res, built_res]
)

# Generate javadoc
custom_target(
	build_by_default : true,
	input : java_files,
	output : 'javadoc',
	command : [
		c_javadoc,
		'-cp', classpath_string,
		'--source-path', '@SOURCE_ROOT@',
		'-d', '@BUILD_ROOT@/javadoc',
		'-charset', 'UTF-8',
		'-encoding', 'UTF-8',
		'-docencoding', 'UTF-8',
		'-source', '17',
		'@INPUT@'
	],
)

# Generate HTML for all markdown files
markdown_docs = ['USERMANUAL.md', 'README.md', 'DEVELOPERS.md', 'external/EXTERNAL.md']
foreach f : markdown_docs
	custom_target(
		build_by_default : true,
		input : f,
		output : '@BASENAME@.html',
		capture : true,
		command : [c_markdown, '@INPUT@']
	)
endforeach

# Copy select files into the root of the build directory
copy_files = [
	'OGAREdit.sh',
	'OGAREdit.command',
	'OGAREdit.bat',
	'external/json.jar',
	'external/commons-imaging.jar',
	'external/twelvemonkeys/imageio-core.jar',
	'external/twelvemonkeys/imageio-jpeg.jar',
	'external/twelvemonkeys/imageio-metadata.jar',
	'external/twelvemonkeys/common-lang.jar',
	'external/twelvemonkeys/common-io.jar',
	'external/twelvemonkeys/common-image.jar',
	'external/ffsampledsp-complete.jar',
	'external/jump3r.jar',
	'external/sqlite-jdbc.jar',
]
foreach f : copy_files
	configure_file(
		input : f,
		output : '@PLAINNAME@',
		copy : true,
	)
endforeach

