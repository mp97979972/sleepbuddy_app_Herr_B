@echo off
git rev-parse --short HEAD > phi_test.txt

Set var=6

set /p HASH=<phi_test.txt
(    
    @echo package com.example.myfirstapp;

    @echo  public class GetNumberCommit {
    @echo  public static String gStr_NumberCommit  = "%HASH%";
    @echo }

)>GetNumberCommit.java

del /f phi_test.txt
