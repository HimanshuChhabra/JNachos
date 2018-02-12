# use normal make for this Makefile
#
# Makefile for building user programs to run on top of Nachos
#
# Several things to be aware of:
#
#    Nachos assumes that the location of the program startup routine (the
# 	location the kernel jumps to when the program initially starts up)
#       is at location 0.  This means: start.o must be the first .o passed 
# 	to ld, in order for the routine "Start" to be loaded at location 0
#

# if you are cross-compiling, you need to point to the right executables
# and change the flags to ld and the build procedure for as

# Syracuse setting. jcoh Jan 11.

GCCDIR = /apps/nachos_gcc/gcc-2.95.2/decstation-ultrix/bin/
# GCCDIR = /usr/ecs/gcc-3.0.3/bin/
LDFLAGS = -T script -N
# ASFLAGS = -mips
ASFLAGS = 
CPPFLAGS = $(INCDIR)


# if you aren't cross-compiling:
# GCCDIR =
# LDFLAGS = -N -T 0
# ASFLAGS =
# CPPFLAGS = -P $(INCDIR)


CC = $(GCCDIR)gcc
# AS = $(GCCDIR)decstation-ultrix-as
AS = $(GCCDIR)as
LD = $(GCCDIR)ld

CPP = /usr/bin/cpp
#INCDIR =-I../userprog -I../threads
CFLAGS = -G 0 -c $(INCDIR)

all:    
#	cd bin; make all 
	make halt 
	make shell 
	make matmult 
	make sort
	make message
	make prog1
	make prog2
	make prog3
	make prog4
	make prog5
	make prog6
	make prog7
	make prog8
	make prog9
	make prog10
	make prog11
	make prog12
	make prog13
	make prog14

start.o: start.s syscall.h
	$(CPP) $(CPPFLAGS) start.s > strt.s
	$(AS) $(ASFLAGS) -o start.o strt.s
	rm strt.s

halt.o: halt.c
	$(CC) $(CFLAGS) -c halt.c
halt: halt.o start.o
	$(LD) $(LDFLAGS) start.o halt.o -o halt.coff
	bin/coff2noff halt.coff halt

shell.o: shell.c
	$(CC) $(CFLAGS) -c shell.c
shell: shell.o start.o
	$(LD) $(LDFLAGS) start.o shell.o -o shell.coff
	bin/coff2noff shell.coff shell

sort.o: sort.c
	$(CC) $(CFLAGS) -c sort.c
sort: sort.o start.o
	$(LD) $(LDFLAGS) start.o sort.o -o sort.coff
	bin/coff2noff sort.coff sort

matmult.o: matmult.c
	$(CC) $(CFLAGS) -c matmult.c
matmult: matmult.o start.o
	$(LD) $(LDFLAGS) start.o matmult.o -o matmult.coff
	bin/coff2noff matmult.coff matmult

message.o: message.c
	$(CC) $(CFLAGS) -c message.c
message: message.o start.o
	$(LD) $(LDFLAGS) start.o message.o -o message.coff
	bin/coff2noff message.coff message

prog1.o: prog1.c
	$(CC) $(CFLAGS) -c prog1.c
prog1: prog1.o start.o
	$(LD) $(LDFLAGS) start.o prog1.o -o prog1.coff
	bin/coff2noff prog1.coff prog1

prog2.o: prog2.c
	$(CC) $(CFLAGS) -c prog2.c
prog2: prog2.o start.o
	$(LD) $(LDFLAGS) start.o prog2.o -o prog2.coff
	bin/coff2noff prog2.coff prog2

prog3.o: prog3.c
	$(CC) $(CFLAGS) -c prog3.c
prog3: prog3.o start.o
	$(LD) $(LDFLAGS) start.o prog3.o -o prog3.coff
	bin/coff2noff prog3.coff prog3

prog4.o: prog4.c
	$(CC) $(CFLAGS) -c prog4.c
prog4: prog4.o start.o
	$(LD) $(LDFLAGS) start.o prog4.o -o prog4.coff
	bin/coff2noff prog4.coff prog4

prog5.o: prog5.c
	$(CC) $(CFLAGS) -c prog5.c
prog5: prog5.o start.o
	$(LD) $(LDFLAGS) start.o prog5.o -o prog5.coff
	bin/coff2noff prog5.coff prog5

prog6.o: prog6.c
	$(CC) $(CFLAGS) -c prog6.c
prog6: prog6.o start.o
	$(LD) $(LDFLAGS) start.o prog6.o -o prog6.coff
	bin/coff2noff prog6.coff prog6

prog7.o: prog7.c
	$(CC) $(CFLAGS) -c prog7.c
prog7: prog7.o start.o
	$(LD) $(LDFLAGS) start.o prog7.o -o prog7.coff
	bin/coff2noff prog7.coff prog7

prog8.o: prog8.c
	$(CC) $(CFLAGS) -c prog8.c
prog8: prog8.o start.o
	$(LD) $(LDFLAGS) start.o prog8.o -o prog8.coff
	bin/coff2noff prog8.coff prog8

prog9.o: prog9.c
	$(CC) $(CFLAGS) -c prog9.c
prog9: prog9.o start.o
	$(LD) $(LDFLAGS) start.o prog9.o -o prog9.coff
	bin/coff2noff prog9.coff prog9

prog10.o: prog10.c
	$(CC) $(CFLAGS) -c prog10.c
prog10: prog10.o start.o
	$(LD) $(LDFLAGS) start.o prog10.o -o prog10.coff
	bin/coff2noff prog10.coff prog10

prog11.o: prog11.c
	$(CC) $(CFLAGS) -c prog11.c
prog11: prog11.o start.o
	$(LD) $(LDFLAGS) start.o prog11.o -o prog11.coff
	bin/coff2noff prog11.coff prog11

prog12.o: prog12.c
	$(CC) $(CFLAGS) -c prog12.c
prog12: prog12.o start.o
	$(LD) $(LDFLAGS) start.o prog12.o -o prog12.coff
	bin/coff2noff prog12.coff prog12

prog13.o: prog13.c
	$(CC) $(CFLAGS) -c prog13.c
prog13: prog13.o start.o
	$(LD) $(LDFLAGS) start.o prog13.o -o prog13.coff
	bin/coff2noff prog13.coff prog13

prog14.o: prog14.c
	$(CC) $(CFLAGS) -c prog14.c
prog14: prog14.o start.o
	$(LD) $(LDFLAGS) start.o prog14.o -o prog14.coff
	bin/coff2noff prog14.coff prog14

clean: 
	/bin/tcsh -cf "rm -f  {*.o,*.coff} bin/{coff2flat,coff2noff,disassemble,*.o}"

