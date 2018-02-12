/* halt.c
 *	Simple program to test whether running a user program works.
 *	
 *	Just do a "syscall" that shuts down the OS.
 *
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automati
*/

#include "syscall.h"

char sender[] = "test/prog7";
char answer[] = "Answer from Prog8";
char *messagePtr;
int bufferID = -1;
int result = -1;
int
main()
{
     bufferID =  WaitMessage(sender,messagePtr,bufferID);
     Exit(9998);

  return 0;
}


