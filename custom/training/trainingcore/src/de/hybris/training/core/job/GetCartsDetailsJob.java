package de.hybris.training.core.job;

import de.hybris.platform.acceleratorservices.email.EmailService;
import de.hybris.platform.acceleratorservices.model.email.EmailAddressModel;
import de.hybris.platform.acceleratorservices.model.email.EmailAttachmentModel;
import de.hybris.platform.acceleratorservices.model.email.EmailMessageModel;
import de.hybris.platform.catalog.CatalogService;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.training.core.service.TestCartsService;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GetCartsDetailsJob extends AbstractJobPerformable {

    @Resource
    private CatalogService catalogService;

    @Resource
    private CatalogVersionService catalogVersionService;

    @Resource
    private EmailService emailService;

    @Resource
    private TestCartsService testCartsService;

    @Resource
    private ModelService modelService;

    @Override
    public PerformResult perform(final CronJobModel arg0) {
        String resultoutput = "Cart Details \n";

        final List<CartModel> model = testCartsService.getCartsDetails();

        for (final CartModel m : model) {
            resultoutput += createCSVRow(m);
        }

        sendEmail(resultoutput, model);

        return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
    }

    private void sendEmail(final String resultoutput, final List<CartModel> model) {
        final EmailAddressModel fromEmail = new EmailAddressModel();
        fromEmail.setEmailAddress("yourshopingmall@icloud.com");
        fromEmail.setDisplayName("No-Reply ");

        final List<EmailAddressModel> toEmails = new ArrayList<EmailAddressModel>();
        final EmailAddressModel toEmail = new EmailAddressModel();
        toEmail.setEmailAddress("usermail@gmail.com");
        toEmail.setDisplayName("user");

        final List<EmailAddressModel> ccEmails = new ArrayList<EmailAddressModel>();
        final EmailAddressModel ccEmail = new EmailAddressModel();
        ccEmail.setEmailAddress("whereismyfood@gmail.com");
        ccEmail.setDisplayName("foodnow");

        toEmails.add(toEmail);
        ccEmails.add(ccEmail);

        final String body = "Cart Details";
        final EmailMessageModel emailModel = emailService.createEmailMessage(toEmails, ccEmails, null, fromEmail,
                "yourshoppingmall@icloud.com", "Cart Details ", body, createAttachment(resultoutput));
        emailService.send(emailModel);

        modelService.remove(fromEmail);
        modelService.removeAll(toEmails);
        modelService.removeAll(ccEmails);
        modelService.removeAll(emailModel);
    }

    private List<EmailAttachmentModel> createAttachment(final String formattedRecipes) {
        final List<EmailAttachmentModel> attachments = new ArrayList<>();
        final Date currentdate = new Date();
        catalogVersionService.setSessionCatalogVersion("apparel-ukContentCatalog", "Online");
        final DataInputStream dataStream = new DataInputStream(
                new ByteArrayInputStream(formattedRecipes.getBytes(StandardCharsets.UTF_8)));
        final EmailAttachmentModel model = emailService.createEmailAttachment(dataStream,
                "CartDetails" + currentdate.toString() + ".csv",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        model.setCatalogVersion(catalogVersionService.getCatalogVersion("apparel-ukContentCatalog", "Online"));
        attachments.add(model);
        return attachments;
    }

    protected String createCSVRow(final CartModel item) {
        final String row = "," + item.getCode() + "\n";
        return  row;
    }
}
