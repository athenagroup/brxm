/* Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com) */

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import {
  MatIconModule,
  MatIconRegistry,
} from '@angular/material';
import { DomSanitizer } from '@angular/platform-browser';

@NgModule({
  exports: [
    MatIconModule,
    HttpClientModule,
  ],
})
export class MaterialModule {
  constructor(
    iconRegistry: MatIconRegistry,
    donSanitizer: DomSanitizer,
  ) {
    const iconsList = [
      'br-logo',
      'audiences',
      'audiences.active',
      'categories',
      'categories.active',
      'documents',
      'documents.active',
      'document-search',
      'document-search.active',
      'experience-manager',
      'experience-manager.active',
      'fast-travel',
      'fast-travel.active',
      'insights',
      'insights.active',
      'projects',
      'projects.active',
      'seo',
      'seo.active',
      'settings',
      'settings.active',
      'site-search',
      'site-search.active',
      'widget',
      'widget.active',
      'extensions',
      'help',
      'user',
      'nav-collapse',
      'nav-expand',
    ];

    iconsList.forEach(icon => iconRegistry.addSvgIcon(
      icon,
      donSanitizer.bypassSecurityTrustResourceUrl(`assets/menu-icons/${icon}.svg`)),
    );
  }
}
