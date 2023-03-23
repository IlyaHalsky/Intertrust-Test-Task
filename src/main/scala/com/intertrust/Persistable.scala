package com.intertrust

sealed trait Persistable

trait PersistableEvent extends Persistable

trait PersistableState extends Persistable
