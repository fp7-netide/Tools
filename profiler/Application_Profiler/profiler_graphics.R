library(ggplot2)
#setwd("/home/tamu/netide/Tools/Engine/ryu-backend")
#setwd("/home/tamu/netide/ryu/ryu/cmd")
trazas <- read.table(text=readLines('statistics.txt')[count.fields('statistics.txt', blank.lines.skip=FALSE) == 6], header = TRUE)
qplot(trazas$filename.lineno.function., trazas$percall.1) + theme(axis.text.x = element_text(angle = 90)) + ylab("Execution Time (seconds)") + xlab("Function Name") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))



ggplot(trazas, aes(percall.1)) + geom_bar(aes(fill = ..count..) ) + scale_fill_gradient("Number of functions", low = "yellow", high = "red") + ylab("Number of functions") + xlab("Execution Time (seconds)") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))






ggplot(trazas, aes(filename.lineno.function., percall.1, fill=percall.1)) + scale_fill_gradient("Seconds", low = "yellow", high = "red") + geom_bar(stat="identity", position="dodge") + theme(axis.text.x = element_text(angle = 90)) + ylab("Execution Time (seconds)") + xlab("Function Name") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))

ggplot(trazas, aes(filename.lineno.function., percall.1, fill=percall.1)) + geom_dotplot(binaxis = "y", binwidth = 8.5) + scale_fill_gradient("Seconds", low = "yellow", high = "red") + theme(axis.text.x = element_text(angle = 90)) + ylab("Execution Time (seconds)") + xlab("Function Name") + theme(axis.title.x = element_text(face="bold", colour="#990000", size=14)) + theme(axis.title.y = element_text(face="bold", colour="#990000", size=14))



