library(ggplot2) #If you don't have it, install this library first with "Tools -> Install packages"

#setwd("/path/to/where/statistics.txt/is") #Uncomment and set your environment path if you haven't already (to check for the statistics.txt file)

trazas <- read.table(text=readLines('statistics.txt')[count.fields('statistics.txt', blank.lines.skip=FALSE) == 6], header = TRUE)
qplot(trazas$filename.lineno.function., trazas$percall.1) + theme(axis.text.x = element_text(angle = 90)) + ylab("Execution Time (seconds)") + xlab("Function Name") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))

#Prints a cumulative histogram with the most and less frequent times of execution
ggplot(trazas, aes(percall.1)) + geom_histogram(aes(fill = ..count..), binwidth = 9) + scale_fill_gradient("Number of functions", low = "yellow", high = "red") + ylab("Number of functions") + xlab("Execution Time (seconds)") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))
#If geom_histogram does not work (you have an old version of the library), you can use geom_bar(aes(fill = ..count..), binwidth = 9) instead 



#Prints the different functions called and their executions times (it only shows the 60 highest values)
ggplot(trazas, aes(filename.lineno.function., percall.1, fill=percall.1)) + scale_fill_gradient("Seconds", low = "yellow", high = "red") + geom_bar(stat="identity", position="dodge") + theme(axis.text.x = element_text(angle = 90)) + ylab("Execution Time (seconds)") + xlab("Function Name") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))
#Same than before but with another value
ggplot(trazas, aes(filename.lineno.function., percall.1, fill=percall.1)) + geom_dotplot(binaxis = "y", binwidth = 8.5) + scale_fill_gradient("Seconds", low = "yellow", high = "red") + theme(axis.text.x = element_text(angle = 90)) + ylab("Execution Time (seconds)") + xlab("Function Name") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))



