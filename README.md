Italian model generator
=============================

---
**NOTE**

This 8 years old project is **archived** because there are now better tools to do it. In particular, [Wiktextract](https://github.com/tatuylonen/wiktextract) provides an excellent source of verb conjugations, PoS tags and of course a dictionary.

Regarding the pattern matching part, have a look at [SpaCy](https://spacy.io/), it is the way to go for NLP tasks in Python, and supports Italian.

---


This program uses various sources (WordNet, ConceptNet, and en.wiktionary) in order to generate different dataset regarding the Italian language, specifically:

 * A basic English -> Italian dictionary (this is actually just a precondition for the other steps)
 * A list of Italian hyponym/hypernym
 * A list of Italian PoS tags
 * A list of Italian verb conjugations

The data is produced both as TSV files and a single SQLLite database
