**************************************************************************************************************************************
****************************************************** Room booking application ******************************************************
**************************************************************************************************************************************

** Student: Alexandru Sorici
** Advisors: Olivier Boissier, Gauthier Picard, Andrea Santi

#### APPLICATION README ####
This readme will explain how to launch the application and how to change simulation parameters.

## 1) Launching the application
The source code is included in the archive containing this readme file.

The application can be launched in 3 ways. You can:
 a) make an eclipse project from the un-zipped source files. Run the application from eclipse by setting the following Run Configuration:
        - Main-Class: roombooking.test.RunMASRoombooking
        - Arguments: the path to the application mas2j file relative to the eclipse project directory (should be AmI-Room-Booking/room-booking-test.mas2j)
 
 b) Go to the directory where the un-zipped project resides and enter the AmI-Room-Booking folder. Execute the command "ant" to run the build.xml script which will launch the application.
 
 c) If neither eclipse nor ant are available you can do the following. Go to the directory where the un-zipped project resides. Execute the command:

   java -cp AmI-Room-Booking/bin/:AmI-Room-Booking/lib/cartago.jar:AmI-Room-Booking/lib/jacaarduino.jar:AmI-Room-Booking/lib/jcommon-1.0.16.jar:AmI-Room-Booking/lib/log4j.jar:AmI-Room-Booking/lib/c4jason.jar:AmI-Room-Booking/lib/jason.jar:AmI-Room-Booking/lib/jfreechart-1.0.13.jar:AmI-Room-Booking/lib/moise.jar jason.infra.centralised.RunCentralisedMAS AmI-Room-Booking/room-booking-test.mas2j


The application will launch the Jason Console. At first the organization will be instantiated. When this stage is completed the simulation will start 
and the GUI with the monitoring charts will appear.


## 2) Setting simulation parameters
The simulation parameters can be specified in the AmI-Room-Booking/src/ami-room-booking-sim.xml file.

The following gives the modifiable parameters and their meaning:
    - the tag <org-agents> lists all the organization's agents (the ones that do the managing and monitoring of the building)
      Each <agent> has a name and a number (which shows how many such agents there are in the organization)
      For the simulation to work correctly, the agent names listed here and their numbers must match exactly those given in the mas2j file.

    - the tag <user-agents> is a container for agents that propose and participate to events.
      The tag <proposers> holds the proposer agents. The names must again match those given in the mas2j file.
      The number of "user_agent" must be high enough such that it can cover the amount of possible concurrent events ( = the total number of room_agents)
      The tag <participant> holds the participants. The name of the agent must match that given in the mas2j. Since these are used only to simulate the
      "register to event" actions, their number is not relevant.

    - the <sim-config> tag holds the important simulation configurations.

      <day-duration> sets the number of days (in simulated time) that the application should run. Mind the fact that in the current version one hour
      is equal to 10 seconds (1h = 10s), this being hard-coded.

      <room-management> gives the definition of individual room properties and the initial distribution of room_agents to their roles. 
        - the <room-data> tag gives information about each <room>, specifying its id, format(layout), no. of seats and equipment IDs (which for now
        are hardcoded in the simulation)

        - the <initial-assignment> tags show which agent is assigned to which role in which room.
        The information from these tags will be made known to the agents of the room booking application via the "observable properties" mechanism of
        the Simulation artifact.
      
      <request-management> tag sets the total number of requests of each type (teachingEvent, meetingEvent, brainstormEvent) and their distribution
      on an intra-day and inter-day basis. An example explanation will be given next:

            <teachingEvent number="6" distIntraDay="uniform" distInterDay="uniform" />
            There are a total of 6 events to be scheduled within the number of days specified by "<day-duration>". The inter-day distribution is uniform
            meaning that there will be 6/num_days teaching requests made each day. 
            The intra-day distribution is also uniform. Request are made in the simulation from 8 to 13 inclusive (6 hours). It means that the number of
            request within a day will be uniformly distributed in the interval 8-13.

            <meetingEvent number="76" distIntraDay="arithmeticDecrease" distInterDay="arithmeticIncrease" />
            There are a total of 76 requests to be scheduled within the number of days specified by "<day-duration>" (2 days default simualtion value).
            The inter-day distribution is set to "arithmeticIncrease". This means that the number of requests per day is going to be given by an
            increasing arithmetic progression the sum of elements of which will be equal to 76.
            The intra-day distribution is set to "arithmeticDecrease". This says that the number of requests within each day will be given by 
            a decreasing arithmetic progression with 6 elements, the sum of which will be equal to the number of events that need to be scheduled that
            day. In this case, it practilcally means there will be more requests made in the morning than close to noon.


In the application, requests are made each day between 8 and 13 o'clock. After that, one can follow how the agents start handling the events either in 
the log window of the application GUI or by watching the print statements written in the Jason Console.
            
