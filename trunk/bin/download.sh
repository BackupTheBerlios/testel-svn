#!/bin/bash

TESTEL='java -Djava.ext.dirs=lib -jar TestEl.jar'
PRETRAINED=corpus/html/pretrained/
MYDIR=../data/trainingsdateien
NOTAGDIR=../data/ohnetags

#Zeit-Artikel
$TESTEL --normalize-only \
	--input='http://images.zeit.de/text/online/2008/05/usa-vorwahl-south-carolina-vorbericht' \
	--encoding=iso-8859-1 --output=$NOTAGDIR/01-zeit.html

#wocadi
$TESTEL --normalize-only \
	--input='http://pi7.fernuni-hagen.de/forschung/wocadi/wocadi_demo_de.html' \
	--encoding=iso-8859-1 --output=$NOTAGDIR/02-wocadi.html

#Zauberwürfenanleitung
$TESTEL --normalize-only \
	--input='http://www.keks.de/wuerfel/' \
	--encoding=iso-8859-1 --output=$NOTAGDIR/03-wuerfel.html

#botanischer Garten
$TESTEL --normalize-only \
	--input='http://www2.uni-jena.de/biologie/spezbot/botgar/gesch.html' \
	--encoding=iso-8859-1 --output=$NOTAGDIR/04-garten.html

#LaTeX-Anleitung
$TESTEL --normalize-only \
	--input='http://www.weinelt.de/latex/subparagraph.html' \
	--encoding=iso-8859-1 --output=$NOTAGDIR/05-latex.html

#Spiegel
$TESTEL --normalize-only \
	--input='http://www.spiegel.de/panorama/justiz/0,1518,druck-531990,00.html' \
	--encoding=iso-8859-1 --output=$NOTAGDIR/06-spiegel.html

#H-SOZ-KULT-Rezension
$TESTEL --normalize-only \
	--input='http://hsozkult.geschichte.hu-berlin.de/rezensionen/type=rezbuecher&id=8569&view=print' \
	--encoding=utf-8 --output=$NOTAGDIR/07-hsozkult.html

#Heise-Artikel
$TESTEL --normalize-only \
	--input='http://www.heise.de/resale/artikel/print/100325' \
	--encoding=utf-8 --output=$NOTAGDIR/08-heise.html

