# flavium

An arena for programming contests.

Flavium provides a web side (built ontop of Ktor and Kotlin) which provides a simple environment to let students compete
their programming on a hard computer science.

## Getting Started

Compile it using gradle:

```shell
$ gradle shadowJar
```

Use the shadowJar (which contains all dependencies) together with a start script (e.g., [server.sh](kuromasu/server.sh)) to start up the server. 


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

**Processing** Each submission is evaluated exclusively on the same host as the server. This means, that only one evaluation runs at the same time. Currently, we have no distributed worker queue. Evaluation of a submission happens in the following steps:

1. Submission is received and stored at the bottom of the task queue.
2. The uploaded submission is established  in the configured work directory
3. The reset script is activated given the work directory. This script established the environment and deletes previous results or files. 
4. The run script then executes the final benchmark. Its execution time is the runtime in the leaderboard. The output of the run script is searched for the regex of the score. `stdout` and `stderr` are captured and stored for later inspection. 
5. Entry in the leaderboard is created.
