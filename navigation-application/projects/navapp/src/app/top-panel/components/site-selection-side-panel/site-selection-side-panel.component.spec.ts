/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { DebugElement, SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatIconModule, MatTreeModule } from '@angular/material';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Site } from '@bloomreach/navapp-communication';
import { PerfectScrollbarModule } from 'ngx-perfect-scrollbar';

import { SiteSelectionSidePanelComponent } from './site-selection-side-panel.component';

describe('SiteSelectionSidePanelComponent', () => {
  let component: SiteSelectionSidePanelComponent;
  let fixture: ComponentFixture<SiteSelectionSidePanelComponent>;
  let de: DebugElement;

  const mockSites: Site[] = [
    {
      siteId: -1,
      accountId: 1,
      name: 'www.company.com',
      subGroups: [
        {
          siteId: 1,
          accountId: 1,
          name: 'UK & Germany',
          subGroups: [
            {
              siteId: 2,
              accountId: 1,
              name: 'Office UK',
            },
            {
              siteId: 3,
              accountId: 1,
              name: 'Office DE',
            },
          ],
        },
      ],
    },
    {
      siteId: -1,
      accountId: 2,
      name:
        'An example company that has a very long name and a subgroup with many items',
      subGroups: [
        {
          siteId: 4,
          accountId: 2,
          name: 'Sub company 001',
        },
        {
          siteId: 5,
          accountId: 2,
          name: 'Sub company 002',
        },
        {
          siteId: 6,
          accountId: 2,
          name: 'Sub company 003',
        },
      ],
    },
  ];

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [
        PerfectScrollbarModule,
        FormsModule,
        MatTreeModule,
        MatIconModule,
        NoopAnimationsModule,
      ],
      declarations: [SiteSelectionSidePanelComponent],
    }).createComponent(SiteSelectionSidePanelComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;
  });

  beforeEach(() => {
    component.sites = mockSites;
    component.ngOnChanges({
      sites: new SimpleChange(undefined, mockSites, true),
    });
    fixture.detectChanges();
  });

  it('should show the tree of sites', () => {
    const sites = de.queryAll(By.css('.caption'));

    expect(sites.length).toBe(2);
    expect(sites[0].nativeElement.textContent.trim()).toBe('www.company.com');
    expect(sites[1].nativeElement.textContent.trim()).toBe(
      'An example company that has a very long name and a subgroup with many items',
    );
  });

  it('should show filter out sites', () => {
    component.searchText = 'Sub company';
    component.ngOnChanges({
      searchText: new SimpleChange(undefined, component.searchText, true),
    });
    fixture.detectChanges();

    const sites = de.queryAll(By.css('.caption'));

    expect(sites.length).toBe(1);
    expect(sites[0].nativeElement.textContent.trim()).toBe(
      'An example company that has a very long name and a subgroup with many items',
    );
  });

  describe('the active node', () => {
    beforeEach(() => {
      component.selectedSite = {
        siteId: 3,
        accountId: 1,
        name: 'Office DE',
      };
      component.ngOnChanges({
        selectedSite: new SimpleChange(undefined, component.selectedSite, true),
      });
      fixture.detectChanges();
    });

    it('should be marked with ".active" class in the DOM', () => {
      const expected = 'Office DE';
      let actual: string;

      const activeLeafNode = de.query(By.css('.active'));

      expect(activeLeafNode).toBeDefined();

      actual = activeLeafNode.nativeElement.textContent.trim();

      expect(actual).toBe(expected);
    });
  });
});
