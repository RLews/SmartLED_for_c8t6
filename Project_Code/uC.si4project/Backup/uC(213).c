/*
************************************************************************************************************************
* file : E:\KeilWorkspace\M3\uCOS_III_mdk5\ApplicationLayer\src\uC.c
* Description : 
* Owner : Lews Hammond
* Time : 2019-6-4
************************************************************************************************************************
*/


#include "uC.h"

#if (D_UC_OS_III_ENABLE == D_SYS_STD_ON)

/* Start Task */
#define D_SYSTEM_START_TASK_PRIO		3
#define D_SYSTEM_START_TASK_STACK_SIZE	128
#define D_SYSTEM_START_TASK_MAX_MSG_NUM	0
#define D_SYSTEM_START_TICK				0
#define D_SYSTEM_START_OPT				(D_OSAL_OPT_TASK_STK_CHK | D_OSAL_OPT_TASK_STK_CLR)

static OSAL_TCB startTaskTCB = {0};
static OSAL_CPU_STACK startTaskStack[D_SYSTEM_START_TASK_STACK_SIZE] = {0};

#endif
static void AppStartTask(void);

/*
************************************************************************************************************************
*                                               Main Function
*
* Description : All model initial and start os.
*
* Arguments   : void.
*
* Returns     : int. Never return.
************************************************************************************************************************
*/
int main(void)
{
#if (D_UC_OS_III_ENABLE == D_SYS_STD_ON)
	OSAL_ERROR tErr = (OSAL_ERROR)0;
#endif
	D_OSAL_ALLOC_CRITICAL_SR();

	Hal_SysInit();
#if (D_SRV_COMM_ENABLE == D_SYS_STD_ON)
	Srv_SysCommInit();
#endif

#if (D_UC_OS_III_ENABLE == D_SYS_STD_ON)
	Osal_OsInit();
#endif

	D_OSAL_ENTER_CRITICAL();
	
#if (D_UC_OS_III_ENABLE == D_SYS_STD_ON)
	D_OSAL_CREATE_TASK_FUNC((OSAL_TCB *)&startTaskTCB,
							(OSAL_CHAR *)"System_Start",
							(OSAL_TASK_FUNC_PTR)AppStartTask,
							(void *) 0,
							(OSAL_PRIO)D_SYSTEM_START_TASK_PRIO,
							(OSAL_CPU_STACK *)&startTaskStack[0],
							(OSAL_CPU_STK_SIZE)(D_SYSTEM_START_TASK_STACK_SIZE / 10),
							(OSAL_CPU_STK_SIZE)D_SYSTEM_START_TASK_STACK_SIZE,
							(OSAL_MSG_QTY)D_SYSTEM_START_TASK_MAX_MSG_NUM,
							(OSAL_TICK)D_SYSTEM_START_TICK,
							(void *)0,
							(OSAL_OPT)D_SYSTEM_START_OPT,
							(OSAL_ERROR *)&tErr
	);
							
	D_OSAL_EXIT_CRITICAL();
	#if (D_SRV_COMM_ENABLE == D_SYS_STD_ON)
	Srv_SysCommTransmit(D_COMM_APP_TRANS_ID, (uint8_t *)"Starting...", sizeof("Starting..."));
	#endif
	D_OSAL_START_SCHE(&tErr);

    return 0;

#else
	AppStartTask();
	while (1)
	{
		SystemLedFlash();
		WifiTaskHandle();
	}

#endif
	
}


/*
************************************************************************************************************************
*                                               Start Task Function
*
* Description : inital os model and create others task.
*
* Arguments   : void.
*
* Returns     : void.
************************************************************************************************************************
*/
static void AppStartTask(void)
{
#if (D_UC_OS_III_ENABLE == D_SYS_STD_ON)

	OSAL_ERROR tErr = (OSAL_ERROR)0;
	
	Osal_StartTaskConfig();
#endif

	/* Create Application Task */
#if (D_SYS_COMM_ENABLE == D_SYS_STD_ON)
	SysCommInit();
#endif
#if (D_FILE_SYSTEM_ENABLE == D_SYS_STD_ON)
	FilesTaskInit();
#endif
	WifiTaskInit();
	
	SystemLedInit();
#if (D_UC_OS_III_ENABLE == D_SYS_STD_ON)
	D_OSAL_TASK_DEL_FUNC((OSAL_TCB *)0, (OSAL_ERROR *)&tErr);//delete self
#endif
	
}

