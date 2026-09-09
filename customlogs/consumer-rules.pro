# A dedução de tag do println() lê a stack trace do chamador. Sem os nomes de arquivo e linha
# preservados, a tag deduzida em release vira ruído — e a stack dos relatórios do Crashlytics
# também. O app já precisa disto para o Crashlytics; a regra vai junto da library para o caso
# de um consumidor que ainda não a tenha.
-keepattributes SourceFile,LineNumberTable
