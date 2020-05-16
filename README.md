![](https://raw.githubusercontent.com/camillewalim/cw-github-pharmacon/master/src/main/resources/github-pharmacon.svg)
# Github Pharmakon
## Etymology
**Pharmakon**: a composite of three meanings: remedy, poison, and scapegoat. 
The first and second senses refer to the everyday meaning of pharmacology (and to its sub-field, toxicology), 
deriving from the Greek source term φάρμακον (phármakon), denoting any drug, 
while the third sense refers to the pharmakos ritual of human sacrifice.

In other words, the best remedy is often the poison itself (by getting used to it).

## Definition
Spring Boot runner application that let you leak sensitive data out of a github server (either 
public or enterprise), and consolidate them in a report.

## Objective
As the name state, the program could both hurt and cure a system as it reveals sensitive 
data.

On my side I use it for educational purpose, in order to detect the failures before someone malevolent 
does. 

I do not take any responsibility in misuse of this program by anyone.

## Working behavior
The algorithm will go through six phases :
 - I: use github public API to get all repositories references & owners
 - II: clone each git locally
 - III: use jgit : 
    - to parse the commits tree in reverse order (starting from HEAD)
    - compute diff for all consecutive commits
    - regex those diff to see if url, username, passwords, secrets, jdbc, etc have been 
      removed or override and retrieve the original value.
    - store and index all security failures
    - compute the public url accessible through browser for all security failures
 - IV: *(optional)* for any security failure (triple of address, username & credential identified), 
   connect, and if successfully: 
   
	   - jdbc : connect & list all databases names, tables, users
	   - api keys : connect & retrieve a valid token out of a oath store.
 - V: *(optional)* name and shame by sending emails to the owner/team.
 - VI: consolidate a report with all credentials found, by owner and repository, as well 
   as statistics to try to identify some pattern in laziness. 

## What is the exploit
Normally developers are not supposed to push credentials on their git repository. 
But with the outcome of cloud computing (docker, virtual machines, etc), their environment 
is not that easy to set up. As one needs to set up some vault remotely for the credentials 
to let their code pick it up at runtime.

And the deadlines are the deadlines, there is a lot of pressure in work nowadays, and 
to speed up their environment set up, they may be tempted to first establish their 
environment without vault and putting credentials in the code base directly. Then they 
will delete those credentials in the code base and secure them in an external vault.

The algorithm uses a relatively well known [exploit](https://www.perforce.com/blog/vcs/how-secure-git) called 
"insecure directories", using the meta data stored in .git folder to re-compute 
the whole history of developments and look for sensitive data that has been deleted 
(but that is still present in this meta data file...).

## What is the risk
Let's say you are a Fortune 500 company *(for the figures)*. 

Here is the risk :

 - __6:00 am__ : some hackers get access to your intranet, and start to look for your 
   github enterprise location.
 - __6:10 am__ : [URL guessing & crawling algorithm](https://books.google.com.hk/books?id=qLzoWKp2JHcC&pg=PA428&lpg=PA428&dq=url+guessing&source=bl&ots=JhxqilFJEU&sig=ACfU3U00dB_Lr5L6kx9gawt-7MvHUg48xg&hl=en&sa=X&ved=2ahUKEwih8L6I17jpAhUDE6YKHc-cBM0Q6AEwCXoECAoQAQ#v=onepage&q=url%20guessing&f=false) finished the job. they found it. 
 - __6:12 am__ : Phase I:  they got the list of more than 4000 repositories, with owners, size, 
   name, etc. 
 - __7:12 am__ : Phase II: On an average size of 40 mB per repository, with a loading speed of 40 mb/s, they loaded 
   all your repositories. 
 - __7:32 am__ : Phase III: They computed your git history, analyzed it, and 
   got hundreds of credentials.
 - __7:35 am__ : Phase IV: They checked the validity of all credentials. They still 
   have a hundred of valid DB credentials for dev infrastucture and dozen for prod infrastructure.
 - __8:00 am__ : Black Hacking Phase: Your employees arrived at work and discover : 
   DDOS attack on servers with valid oauth secrets, dropped or corrupted or locked DB, 
   messed dev environments, leaked information, etc. 
    
     

## How to avoid the exploit
An exploiter would **not** be able to detect failures if **only one of those six measures is applied**.

As a developer, you may patch your system by :
 - not putting credentials... **ever**
 - changing regularly the credentials so the ones in the git aren't valid anymore
 - making your git private
 - squashing the git history so it will remove the leaking commits. 

As an administrator, you may patch your system by:
 - activating analysis program (such at this one), or a code compliant sonar (sonarqube, 
   sonarlint) so they track for potentially leaks 
   in the server.
 - putting a hard limits on github public api (the default limit being something like 
   5000 request/hour... ).

On the opposite, if it did, that mean you have a serious IT culture problem in your 
business, as you have professional incompetence behavior cumulating.