
# Process resources to prepare for inclusion in OGAREdit.jar

res = [
        'dbDef.sql',
        '__default.jpg',
        '__default.mp3',
        'colorGradient.png',
]

# SVGs that need to be converted to PNGs
svgs = [
	'ico_hoverHelp.svg',
]
built_res = []
foreach f : svgs
	built_res += custom_target(
		build_by_default : true,
		input : f,
		output : '@BASENAME@.png',
		feed : true,
		capture : true,
		command : [c_cairosvg, '-f', 'png', '--output-width=50', '--output-height=50', '-'],
	)
endforeach

copy_res = files(res)
