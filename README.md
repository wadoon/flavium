# flavium

An arena for programming contests.

Flavium provides a web side (built ontop of Ktor and Kotlin) which provides a simple environment to let students compete
their programming on a hard computer science.

## Getting Started

Compile it using gradle:

```shell
$ gradle shadowJar
```

Use the shadowJar (which contains all dependencies) together with a start script (e.g., [kuromasu/server.sh](server.sh)) to start up the server. 


## Internals

**Use Case** The service was developed for having a common competition ground for the SAT exercise of the lecture Formal
System at the Karlsruhe Institute of Technology. This exercise consists of writing a translation of a puzzle instance
onto a SAT problem. The submission is a single Java file.

**Data privacy**
One goal was to be compliant to the data privacy regulation. Therefore, we limit the amount of personal data. So you can
only upload a single file for the submission without further information. Nearly every information is randomly generated
by the server.

Following data exists:

* The content of the uploaded submission (stored until the evaluation)
* Internal id for each submission that is a random UUID. The id should be kept secret because behind this id the stdout
  and stdout of the evaluation are visible.
* A pseudonym given randomly by the server for each submission (publicly visible)
* Run time and score of each evaluated submission, as well as the submission time.

Each submission also creates an entry in the cookie. Inside the cookie we store the pseudonym, internal id and upload
time for each submission.  


