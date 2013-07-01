/* Copyright (C) 2013, Siddharth Gopal (gcdart AT gmail)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of version 2.1 of the GNU Lesser General Public License
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA, 02111-1307, USA,
 * or visit the GNU web site, www.gnu.org.
 */

# This makefile creates the required 'class' files from java files.
# You need to create a JAR file from these class-files and add the 
# appropriate libraries from the libs directory

SOURCES=" ./src/base/PIF.java \
./src/base/PIFArray.java \
./src/base/Example.java \
./src/base/WeightParameter.java \
./src/ml/Mcsrch.java \
./src/ml/LBFGS.java \
./src/ml/LogisticRegression.java \
./src/ml/BinarySVM.java \
./src/hadoop/Converter.java \
./src/hadoop/TrainingDriver.java \
./src/hadoop/TestingDriver.java";

rm -rf ./bin

mkdir -p ./bin

LIBDIR=libs

for src in `echo $SOURCES`;
do 
    javac -d ./bin -classpath .:./libs/*:./bin $src
    echo $src
done


