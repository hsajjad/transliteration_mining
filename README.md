# transliteration_mining
Model to extract translation pairs in an unsupervised, semi-supervised and supervised fashion

### Unsupervised mining - given a list of word pairs
```
java -Dfile.encoding=UTF-8 -jar miner2.jar -unsupervised -train tmp/out.words
```

Model options
http://alt.qcri.org/~hsajjad/software/transliteration_mining/appendix_software.pdf


#### Note: sometimes the word list contains a lot of noise. You may use the following clean script on the word list before giving it to unsupervised miner.
```
https://github.com/moses-smt/mosesdecoder/blob/master/scripts/Transliteration/clean.pl
```


