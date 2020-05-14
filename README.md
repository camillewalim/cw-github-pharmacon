# Github Search and Destroy
Spring Boot runner application that let you leak sensitive data out of a github server (either 
public or private), and report all security failures.

For educational purpose, in order to detect the failures before someone else does... 

The algorithm will :
 - 1: use github public API to get all repositories references & owners
 - 2: clone each git locally
 - 3: use jgit : 
    - to parse the commits tree in reverse order (starting from HEAD)
    - compute diff for all consecutive commits
    - regex those diff to see if url, username, passwords, secrets, jdbc, etc have been 
      removed or override and retrieve the original value.
    - store and index all security failures
 - 4: for any security failure (triple of address, username & credential identified), 
   connect, and if successfully: 
	   - jdbc : list all databases names. 
 - 5: compute the public url accessible through browser for all security failures
 - 6: *(optional)* name and shame by sending emails to the owner/team.
 - 7: consolidate a report with all credentials found, by owner and repository, as well 
   as statistics to try to identify some pattern in laziness. 