package com.gdiama.example.cluster

import akka.cluster.{Member, MemberStatus}
import com.gdiama.example.cluster.Protocol._
import play.api.libs.json.Json._
import play.api.libs.json._

object JsonSerializer {
  implicit val memberStatusJsonSerializer: Format[MemberStatus] = new Format[MemberStatus] {
    override def reads(json: JsValue): JsResult[MemberStatus] =
      throw new UnsupportedOperationException("Reading MemberStatus from json is not supported")

    override def writes(o: MemberStatus): JsValue =
      JsString(
        o match {
          case MemberStatus.Joining => "Joining"
          case MemberStatus.WeaklyUp => "WeaklyUp"
          case MemberStatus.Up => "Up"
          case MemberStatus.Down => "Down"
          case MemberStatus.Exiting => "Exiting"
          case MemberStatus.Leaving => "Leaving"
          case MemberStatus.Removed => "Removed"
        })
  }

  implicit val memberJsonSerializer: Format[Member] = new Format[Member] {
    override def reads(json: JsValue): JsResult[Member] =
      throw new UnsupportedOperationException("Reading Member from json is not supported")

    override def writes(o: Member): JsValue =
      Json.obj(
        "address" -> o.uniqueAddress.address.toString,
        "status" -> o.status,
        "roles" -> o.roles)
  }

  implicit val membershipInfoJsonSerializer: Format[ClusterMembership.MembershipInfo] = Json.format
  implicit val startLiveStreamRequestFmt: Format[SdpOffer] = Json.format[SdpOffer]
  implicit val iceCandidateFmt: Format[IceCandidate] = Json.format[IceCandidate]
  implicit val sdpAnswerFmt: Format[SdpAnswer] = Json.format[SdpAnswer]
  implicit val tempFmt: Format[TempMessage] = Json.format[TempMessage]
  implicit val webRtcFmt: Format[WebRtcMessage] = new Format[WebRtcMessage] {
    def reads(json: JsValue): JsResult[WebRtcMessage] = {

      def fromType(`type`: String, data: JsValue): JsResult[WebRtcMessage] = `type` match {
        case "sdpOffer" => Json.fromJson[SdpOffer](data)
        case "iceCandidate" => Json.fromJson[IceCandidate](data)
        case _ => JsError(s"Unexpected JSON value $json")
      }

      for {
        theType <- (json \ "type").validate[String]
        result <- fromType(theType, json)
      } yield result
    }

    def writes(msg: WebRtcMessage): JsValue = {
      def ofType(value: JsValue, t: String): JsValue = {
        value.as[JsObject].deepMerge(Json.obj("type" -> t))
      }

      msg match {
        case m: SdpOffer => ofType(Json.toJson(m)(startLiveStreamRequestFmt), "sdpOffer")
        case m: SdpAnswer => ofType(Json.toJson(m)(sdpAnswerFmt), "sdpAnswer")
        case m: IceCandidate => ofType(Json.toJson(m)(iceCandidateFmt), "iceCandidate")
        case m: TempMessage => ofType(Json.toJson(m)(tempFmt), "temp")
      }

    }
  }

}
