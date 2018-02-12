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

char sender[] = "test/prog5";
char answer1[] = "Answer1 from Prog6";
char answer2[] = "Answer2 from Prog6";
char *messagePtr;
int bufferID = -1;
int result1 = -1;
int result2 = -1;
int
main()
{
	bufferID =  WaitMessage(sender,messagePtr,bufferID);
        result1  =  SendAnswer(result1,answer1,bufferID);

	bufferID =  WaitMessage(sender,messagePtr,bufferID);
 	result2  =  SendAnswer(result2,answer2,bufferID);

     Exit(9996);

  return 0;
}


