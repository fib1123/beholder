package org.virtuslab.beholder.repositories

import org.virtuslab.beholder.model.{ User, _ }
import org.virtuslab.unicorn.LongUnicornPlay._
import org.virtuslab.unicorn.LongUnicornPlay.driver.simple._

import scala.slick.lifted.TableQuery

class UsersRepository extends BaseIdRepository[UserId, User, Users](TableQuery[Users])