#!/bin/bash
cd "${0%/*}"
java -ea -cp OGAREdit.jar:json.jar:sqlite-jdbc.jar:ffsampledsp-complete.jar:jump3r.jar:commons-imaging.jar:imageio-core.jar:imageio-metadata.jar:common-lang.jar:common-io.jar:common-image.jar:imageio-jpeg.jar net.boerwi.ogaredit.Main "$@"
