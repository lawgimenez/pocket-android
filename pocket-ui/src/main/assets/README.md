## Special notes about Graphik Font

We have edited the graphik font to remove the font leadings and increase the ascent and descent to
fix some line spacing issues.  
Android doesn't respect leadings.
The font em was 1000, leading 200, ascent 818, descent 182. 
So I set the leading to 0, and added 20% to the ascent and descent (because 200 is 20% of 1000). 
So the ascent is now set to 982 and descent 218.


To edit a font, install the program Font Forge https://fontforge.org/en-US/
Open the font file.
Go to Element -> Font Info -> OS/2 -> Metrics.
Make sure "Really use Typo metrics" is unchecked.
Change the HHead Ascent, HHead Descent, and HHead Line Gap values.
Click OK.
Go to File -> Generate Fonts.
Make it an otf file extension.
Make sure "TrueType" is selected.
Click Generate.
Click through any warnings.
Done.
