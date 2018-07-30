# Fruitsea - Design Doc
## Function and Goal
Fruitsea is an application that classifies a camera capture of a fruit as a particular type of fruit, offers some information about the fruit, and gives some tips about how to tell if the fruit is good or not.

This application aims to make fruit identification and selection easier for consumers at groceries, particularly those encountering more exotic fruits for the first time or simply trying to remember how to find good quality fruit.
## Components
### Datasets
Fruits 360 - Mihai Oltean - https://www.kaggle.com/moltean/fruits

### Resources
DeepFruits: A Fruit Detection System Using Deep Neural Networks - https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5017387/
Advanced Convolutional Neural Networks - https://www.tensorflow.org/tutorials/images/deep_cnn

## Prototype Roadmap
### Phase I: Fruit Recognition
* The classifier needs to be trained on a dataset of fruit images to be able to tell if something is clearly a type of fruit or not in a picture taken by a phone.
* Some backend code in Python can run a TensorFlow convolutional neural network to train such a classifier.
* Such a convolutional neural network could be built like in the TensorFlow tutorial, including 2 pooling layers to cut the training image size down to 25x25.
* Captures of fruit from the phone will likely have to be downsampled to 100x100 to be fed into the CNN.
### Phase II: Information Display
* Information concerning characteristics, location, and methods of perusal can be stored on a database for each possible class of fruit defined in the application.
* There is an option to take a photo of a whole fruit for classification. If multiple fruits are close in likelihood according to the classifier, then those fruits are put together in a list for the user to pick. If nothing relevant shows up, a manual search through the database is opened.
