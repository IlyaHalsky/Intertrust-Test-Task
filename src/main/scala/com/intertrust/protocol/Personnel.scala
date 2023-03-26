package com.intertrust.protocol


trait PersonnelCommand

case class PersonError(personId: String, error: String) extends PersonnelCommand