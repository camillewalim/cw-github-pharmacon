# Github Pharmakon
## Etymology
**Pharmakon**: a composite of three meanings: remedy, poison, and scapegoat. 
The first and second senses refer to the everyday meaning of pharmacology (and to its sub-field, toxicology), 
deriving from the Greek source term φάρμακον (phármakon), denoting any drug, 
while the third sense refers to the pharmakos ritual of human sacrifice.

## Definition
Spring Boot runner application that let you leak sensitive data out of a github server (either 
public or enterprise), and consolidate them in a report.

## Objective
As the name state, the program could both hurt and cure a system as it reveals sensitive 
data.

On my side I use it for educational purpose, in order to detect the failures before someone else does. 

I do not take any responsibility in misuse of this program by anyone.

## Working behavior
The algorithm will :
 - 1: use github public API to get all repositories references & owners
 - 2: clone each git locally
 - 3: use jgit : 
    - to parse the commits tree in reverse order (starting from HEAD)
    - compute diff for all consecutive commits
    - regex those diff to see if url, username, passwords, secrets, jdbc, etc have been 
      removed or override and retrieve the original value.
    - store and index all security failures
 - 4: compute the public url accessible through browser for all security failures
 - 5: *(optional)* for any security failure (triple of address, username & credential identified), 
   connect, and if successfully: 
	   - jdbc : connect & list all databases names.
	   - api keys : connect & retrieve a valid token out of a oath store.
 - 6: *(optional)* name and shame by sending emails to the owner/team.
 - 7: consolidate a report with all credentials found, by owner and repository, as well 
   as statistics to try to identify some pattern in laziness. 