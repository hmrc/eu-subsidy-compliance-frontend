package uk.gov.hmrc.eusubsidycompliancefrontend.connectors


import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject, Singleton}
import uk.gov.hmrc.eusubsidycompliancefrontend.models.Error
import uk.gov.hmrc.eusubsidycompliancefrontend.models.emailSend.EmailSendRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SendEmailConnectorImpl])
trait SendEmailConnector {
  def sendEmail(emailSendRequest: EmailSendRequest)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
}

@Singleton
class SendEmailConnectorImpl @Inject() (http: HttpClient, servicesConfig: ServicesConfig)(implicit
                                                                                          ec: ExecutionContext
) extends SendEmailConnector {

  private val baseUrl: String      = servicesConfig.baseUrl("email-send")
  private val sendEmailUrl: String = s"$baseUrl/hmrc/email"

  override def sendEmail(emailSendRequest: EmailSendRequest)(implicit
                                                             hc: HeaderCarrier
  ):Future[Either[Error, HttpResponse]] =
    http
      .POST[EmailSendRequest, HttpResponse](sendEmailUrl, emailSendRequest)
      .map(Right(_))
      .recover { case e => Left(Error(e)) }

}

