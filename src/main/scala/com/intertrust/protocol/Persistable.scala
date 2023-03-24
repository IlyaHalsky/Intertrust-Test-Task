package com.intertrust.protocol

sealed trait Persistable

trait PersistableEvent extends Persistable

trait PersistableState extends Persistable
