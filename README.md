# Multic

This one of the simplest OVA binary classifier parallelization using hadoop.
There are two classifiers which are supported,

a) Linear Binary SVM (using Dual co-ordinate descent , no kernels , sorry )

b) Regularized Logistic Regression 

The classifier for each class is trained in Parallel.

# Building the package

The compile.sh script generated a set of classes. You need to manually
add to a JAR file and add the appropriate libraries. For simplicity,
I've also included an eclipse project file which contains the entire
project.
   
# Package Description

There are basically 3 supported operations

 - 1. Convert the dataset into seqfile format
 - 2. Train a classifier on a dataset in seqfile format
 - 3. Predict the outcome of the classifier on a dataset in seqfile format

## Converting to seqfile Format

Each line of the input dataset must be of the format

<class-label>[,<class-label>]+ [ feature-id:feature-value]+ # <docid-id>

To convert the CLEF dataset (can be downloaded from 
   http://gcdart.blogspot.com/2012/08/datasets_929.html) in the folder, run

hadoop jar MulticlassClassifier.jar hadoop.Converter \
 -D gc.Converter.input=datasets/clef/text/ \
 -D gc.Converter.output=datasets/clef/seqfile \
 -D gc.Converter.name=converter

Where the HDFS path datasets/clef/text/ contains the input dataset.
The output HDFS path contains the same data in a seq-file format

## Training a Classifier

Two types of classifiers are supported - BinarySVM and Regularized Logistic 
Regression. Basically, the mapper trains a classifier for each class-label in 
the dataset in parallel.

To train Binary svm on the seqfile generated above, 

hadoop jar hblr.jar hadoop.TrainingDriver \
       -D gc.TrainingDriver.name=svm-train \
       -D gc.TrainingDriver.dataset=datasets/clef/seqfile/ \
       -D gc.TrainingDriver.output=datasets/clef/params/svm/ \
       -D gc.TrainingDriver.input=datasets/clef/leaflabels/ \
       -D gc.TrainingDriver.classifier=svm \
       -D gc.TrainingDriver.svm.C=1 \
    -D gc.TrainingDriver.svm.eps=.1 \
    -D gc.TrainingDriver.svm.maxiter=1000

This trains a binary-svm for each class-label (separated by newlines) from the 
input file gc.TrainingDriver.input and uses the dataset located at 
gc.TrainingDriver.dataset and stores the resulting weight-vectors at 
gc.TrainingDriver.output. 
The parameters of the SVM are given using gc.TrainingDriver.svm.{C,eps,maxiter}

1. Note that the gc.TrainingDriver.dataset MUST contain a '/' at the end.

2. datasets/clef/leaflabels/ contains a file which has the list of all 
   class-labels (newline separated) present in the dataset.

3. gc.TrainingDriver.svm.{C,eps,maxiter} are the parameters SVM. 
   - C is the regularization term [default value is 1]
     .5*||w||^2 + C \sum\limits_{i=1}^{N} max(1-y_i*(w^T x_i),0)
   - eps is the termination condition
   - maxiter is the maximum number of iterations to run. 

To train a logistic regression,

hadoop jar hblr.jar hadoop.TrainingDriver \
       -D gc.TrainingDriver.name=lr-train \
       -D gc.TrainingDriver.dataset=datasets/clef/seqfile/ \
       -D gc.TrainingDriver.output=datasets/clef/params/lr/ \
       -D gc.TrainingDriver.input=datasets/clef/leaflabels/ \
       -D gc.TrainingDriver.classifier=lr \
       -D gc.TrainingDriver.lr.lambda=.01 \
       -D gc.TrainingDriver.lr.eps=1e-4 \
    -D gc.TrainingDriver.svm.maxnfn=1000

The default value of gc.TrainingDriver.lr.eps is .1, which is insufficient for 
most datasets. Make sure you change to a stricter value like 1e-4 as above.


## Testing a classifier

Ideally you want to test a dataset different than the training set. But here, 
the same training dataset is used

hadoop jar hblr.jar hadoop.TestingDriver \
        -D gc.TestingDriver.name=svm-test \
        -D gc.TestingDriver.dataset=datasets/clef/seqfile/ \
        -D gc.TestingDriver.output=datasets/clef/pred/\
        -D gc.TestingDriver.input=datasets/clef/params/svm/\
	-D gc.TestingDriver.rank=2

This stores the  2 (gc.TestingDriver.rank)  highest scoring class-labels at
location gc.TestingDriver.output of the testing-dataset located at 
gc.TestingDriver.dataset, using the trained weight-vectors located 
at gc.TestingDriver.input.


# ACKNOWLEDGEMENTS


1. LBFGS.java and Msrch.java are the implementation
   of Limited Memory BFGS and associated line search by robert_dodier@yahoo.com
2. BinarySVM.java is a re-implementation of the dual co-ordinate descent
   for L2 regularized L1 Support Vector Machines by 
   http://www.csie.ntu.edu.tw/~cjlin/liblinear/



Siddharth Gopal (gcdart@gmail)
CMU, Pittsburgh

