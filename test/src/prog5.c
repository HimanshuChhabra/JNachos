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

char receiver[] = "test/prog6";
char message1[] = "Message1 from Prog5";
char message2[] = "Message2 from Prog5";
char *answerPtr;
int bufferID = -1;
int result1 = -1;
int result2 = -1;
int
main()
{
	bufferID =  SendMessage(receiver,message1,bufferID);
	result1  =  WaitAnswer(result1,answerPtr,bufferID);
     
	bufferID =  SendMessage(receiver,message2,bufferID);
	result2  =  WaitAnswer(result2,answerPtr,bufferID);

     Exit(9995);

  return 0;
}


