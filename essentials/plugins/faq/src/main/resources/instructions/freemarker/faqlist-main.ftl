<#include "../include/imports.ftl">

<#-- @ftlvariable name="document" type="{{beansPackage}}.FaqList" -->
<#if document??>
  <#if document.FAQ??>
    <div class="has-edit-button">
      <@hst.manageContent document=document/> <!-- edit faq list document -->
      <h1>${document.title?html}</h1>
      <div>
        <@hst.html hippohtml=document.description/>
      </div>
      <#list document.faqItems as faq>
        <div class="has-edit-button">
          <h3><a href="<@hst.link hippobean=faq />">${faq.question?html}</a></h3>
          <@hst.html hippohtml=faq.answer/>
          <@hst.manageContent document=faq/> <!-- edit faq item -->
        </div>
      </#list>
      <div class="has-new-content-button">
        <@hst.manageContent templateQuery="new-faq-item" defaultPath="faq"/> <!-- add faq item -->
      </div>
    </div>
  <#else>
  <div class="alert alert-danger">The selected document should be of type FAQ list.</div>
  </#if>
<#elseif editMode>
  <div class="has-edit-button">
    <img src="<@hst.link path="/images/essentials/catalog-component-icons/faq.png" />"> Click to edit FAQ
    <!-- add faq list document -->
    <@hst.manageContent templateQuery="new-faq-list" componentParameter="document" defaultPath="faq"/>
  </div>
</#if>