package com.river.connector.aws.ses.internal

import com.river.connector.aws.ses.model.*
import software.amazon.awssdk.services.ses.model.BulkEmailDestination
import software.amazon.awssdk.services.ses.model.MessageTag
import software.amazon.awssdk.services.ses.model.SendBulkTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest
import software.amazon.awssdk.services.ses.model.Content as SesContent
import software.amazon.awssdk.services.ses.model.Destination as SesDestination
import software.amazon.awssdk.services.ses.model.SendEmailRequest as SesSendEmailRequest

internal fun Content.asSesContent(): SesContent =
    SesContent
        .builder()
        .apply { data(data).charset(charset.name()) }
        .build()

internal fun Destination.asSesDestination(): SesDestination =
    SesDestination
        .builder()
        .apply {
            toAddresses(to)
            ccAddresses(cc)
            bccAddresses(bcc)
        }
        .build()

internal fun Map<String, String>.asSesTags(): List<MessageTag> =
    map { MessageTag.builder().name(it.key).value(it.value).build() }

internal fun SendEmailRequest.Single<Message.Plain>.asSesSendEmailRequest() =
    SesSendEmailRequest
        .builder()
        .apply {
            tags(tags.asSesTags())

            when (source) {
                is Source.Arn -> sourceArn(source.arn)
                is Source.Plain -> source(source.source)
            }

            when (returnPath) {
                is ReturnPath.Arn -> returnPathArn(returnPath.arn)
                is ReturnPath.Plain -> returnPathArn(returnPath.path)
                null -> {}
            }
        }
        .configurationSetName(configurationSetName)
        .replyToAddresses(replyToAddresses)
        .destination(destination.asSesDestination())
        .message { builder ->
            val subject = message.subject.asSesContent()

            builder
                .subject(subject)
                .body { body ->
                    when (message) {
                        is Message.Html -> body.html(message.body.asSesContent())
                        is Message.Text -> body.text(message.body.asSesContent())
                    }
                }
        }
        .build()

internal fun SendEmailRequest.Single<Message.Template>.asSesTemplatedRequest() =
    SendTemplatedEmailRequest
        .builder()
        .templateData(message.data)
        .apply {
            tags(tags.asSesTags())

            when (source) {
                is Source.Arn -> sourceArn(source.arn)
                is Source.Plain -> source(source.source)
            }

            when (returnPath) {
                is ReturnPath.Arn -> returnPathArn(returnPath.arn)
                is ReturnPath.Plain -> returnPathArn(returnPath.path)
                null -> {}
            }

            when (message.ref) {
                is Message.Template.TemplateRef.Arn -> templateArn(message.ref.arn)
                is Message.Template.TemplateRef.Name -> template(message.ref.name)
            }
        }
        .configurationSetName(configurationSetName)
        .replyToAddresses(replyToAddresses)
        .destination(destination.asSesDestination())
        .build()

internal fun SendEmailRequest.BulkTemplated.asSesBulkTemplatedRequest() =
    SendBulkTemplatedEmailRequest
        .builder()
        .apply {
            defaultTags(defaultTags.asSesTags())

            when (source) {
                is Source.Arn -> sourceArn(source.arn)
                is Source.Plain -> source(source.source)
            }

            when (returnPath) {
                is ReturnPath.Arn -> returnPathArn(returnPath.arn)
                is ReturnPath.Plain -> returnPathArn(returnPath.path)
                null -> {}
            }

            defaultTemplateData(message.data)

            when (message.ref) {
                is Message.Template.TemplateRef.Arn -> templateArn(message.ref.arn)
                is Message.Template.TemplateRef.Name -> template(message.ref.name)
            }
        }
        .destinations(
            destinations.map {
                BulkEmailDestination
                    .builder()
                    .destination(it.destination.asSesDestination())
                    .replacementTags(it.replacementTags.asSesTags())
                    .replacementTemplateData(it.replacementData)
                    .build()
            }
        )
        .configurationSetName(configurationSetName)
        .replyToAddresses(replyToAddresses)
        .build()
