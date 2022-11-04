package edu.kit.iti.formal.flavium

import kotlin.random.Random

// Adjusted version of https://github.com/moby/moby/blob/master/pkg/namesgenerator/names-generator.go
class NameGenerator(takenNamesParam : MutableSet<String>) {

    private val takenNames = takenNamesParam;

    val left = arrayOf(
        "amazing",
        "awesome",
        "bold",
        "brave",
        "correct",
        "clever",
        "competent",
        "confident",
        "cool",
        "determined",
        "eager",
        "elegant",
        "eloquent",
        "epic",
        "fair",
        "focused",
        "friendly",
        "formal",
        "funny",
        "gifted",
        "gracious",
        "great",
        "happy",
        "heuristic",
        "hopeful",
        "infallible",
        "inspiring",
        "intelligent",
        "interesting",
        "kind",
        "laughing",
        "magical",
        "modest",
        "nice",
        "nostalgic",
        "objective",
        "optimistic",
        "peaceful",
        "pedantic",
        "practical",
        "recursing",
        "relaxed",
        "reliable",
        "stoic",
        "secure",
        "trustworthy",
        "wonderful",
    );

    val right = arrayOf(
        "noether",
        "liskov",
        // Jeanette Wing
        "wing",
        "lovelace",
        // Frances E. Allen, became the first female IBM Fellow in 1989. In 2006, she became the first female recipient of the ACM's Turing Award. https://en.wikipedia.org/wiki/Frances_E._Allen
        "allen",
        // Kathleen Booth, she's credited with writing the first assembly language. https://en.wikipedia.org/wiki/Kathleen_Booth
        "booth",
        // Grace Hopper developed the first compiler for a computer programming language and  is credited with popularizing the term "debugging" for fixing computer glitches. https://en.wikipedia.org/wiki/Grace_Hopper
        "hopper",
        // Jean Bartik, born Betty Jean Jennings, was one of the original programmers for the ENIAC computer. https://en.wikipedia.org/wiki/Jean_Bartik
        "bartik",
        // Maria Gaetana Agnesi - Italian mathematician, philosopher, theologian and humanitarian. She was the first woman to write a mathematics handbook and the first woman appointed as a Mathematics Professor at a University. https://en.wikipedia.org/wiki/Maria_Gaetana_Agnesi
        "agnesi",
        // Kathleen Antonelli, American computer programmer and one of the six original programmers of the ENIAC - https://en.wikipedia.org/wiki/Kathleen_Antonelli
        "antonelli",
        // Evelyn Boyd Granville - She was one of the first African-American woman to receive a Ph.D. in mathematics; she earned it in 1949 from Yale University. https://en.wikipedia.org/wiki/Evelyn_Boyd_Granville
        "boyd",
        // Marlyn Wescoff - one of the original programmers of the ENIAC. https://en.wikipedia.org/wiki/ENIAC - https://en.wikipedia.org/wiki/Marlyn_Meltzer
        "wescoff",
        // Shafi Goldwasser - turing award 2012 https://en.wikipedia.org/wiki/Shafi_Goldwasser
        "goldwasser",

        "genzen",
        "russel",
        "goedel",
        "kripke",
        "cook",
        "goodstein",
        "peano",
        "craig",
        "clarke",
        "cousot",
        "shannon",
        "rice",
        "turing",
    );

    fun getName() : String {
        while (true) {
            val leftId = Random.nextInt(left.size);
            val rightId = Random.nextInt(right.size);
            val name = left[leftId].plus("_").plus(right[rightId]);
            if (!takenNames.contains(name)){
                takenNames.add(name);
                return name;
            }
        }

    }
}