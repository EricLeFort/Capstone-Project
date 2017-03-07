/*
 * main.c
 *
 *  Created on: Nov 14, 2011
 *      Author: borzoo
 */


#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>

#include <arpa/inet.h>
#include <assert.h>
#include <errno.h>
#include <signal.h>
#include <string.h>
#include <sys/wait.h>
#include <netdb.h>
#include <unistd.h>

#define MYPORT   1979
#define OTHERPORT   1976
#define BUFLEN 2076

int main()
{

	struct sockaddr_in si_me, si_other;

	int s;
	char rcvBuf[BUFLEN];
	char sndBuf[BUFLEN];
	char strIP[BUFLEN];
	char myStr[BUFLEN];
	int slen = sizeof(si_other);

	s = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
	if (s == -1){
		exit(0);
	}
	else
		printf("UDP Socket created successfully.\n");

/*	memset((char *) &si_me, 0, sizeof(si_me));
	si_me.sin_family = AF_INET;
	si_me.sin_port = htons(MYPORT);
	si_me.sin_addr.s_addr = htonl(INADDR_ANY);
    

	int retVal = bind(s, (struct sockaddr*)&si_me, sizeof(si_me));
	if (retVal == -1){
		exit(0);
	}
	else
		printf("Socket bound successfully.\n");*/


	/*************************/
	printf("Enter IP address of the receiver:");
	scanf("%s", strIP);
//	printf("%s %s\n", strIP, myStr);

	struct sockaddr_in sis;
	int sislen=sizeof(sis);
	memset((char *) &sis, 0, sizeof(sis));
	sis.sin_family = AF_INET;
	sis.sin_port = htons(OTHERPORT);
	inet_aton(strIP, &sis.sin_addr);

    while (1) {
        printf("Enter a message:");
        scanf("%s", myStr);
        sendto(s, myStr, sizeof(myStr), 0, (struct sockaddr *)&sis, sislen);
    }

	close(s);
	return 0;
}
