/*
 * Copyright 2016-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.calendar.ical;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import io.spring.calendar.release.ProjectReleases;
import io.spring.calendar.release.Release;
import io.spring.calendar.release.Release.Status;

import org.springframework.stereotype.Component;

/**
 * A {@link Supplier} of {@link ProjectReleases} for {@link ICalProject ICalProjects}.
 *
 * @author Andy Wilkinson
 */
@Component
class ICalProjectReleasesSupplier implements Supplier<List<ProjectReleases>> {

	private final List<ICalProject> projects = createProjects();

	@Override
	public List<ProjectReleases> get() {
		return this.projects.stream().map(this::createProjectReleases).collect(Collectors.toList());
	}

	private ProjectReleases createProjectReleases(ICalProject project) {
		List<Release> releases = parseICalendars(project) //
				.stream() //
				.flatMap((calendar) -> calendar.getEvents().stream()).map((event) -> createRelease(project, event))
				.collect(Collectors.toList());
		return new ProjectReleases(project.getName(), releases);
	}

	private List<ICalendar> parseICalendars(ICalProject project) {
		InputStream inputStream = null;
		try {
			inputStream = project.getCalendarUrl().openStream();
			return Biweekly.parse(inputStream).all();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				}
				catch (IOException ex) {
					// Continue
				}
			}
		}
	}

	private Release createRelease(ICalProject project, VEvent event) {
		String name = event.getSummary().getValue();
		if (name.startsWith(project.getName())) {
			name = name.substring(project.getName().length()).trim();
		}
		return new Release(project.getName(), name,
				new SimpleDateFormat("yyyy-MM-dd").format(event.getDateStart().getValue()), Status.UNKNOWN, null);
	}

	private static List<ICalProject> createProjects() {
		try {
			return Arrays.asList(new ICalProject("Spring Data", new URL(
					"https://outlook.office365.com/owa/calendar/9d3cecb6098e4d7d884561cf288d70b7@vmware.com/4f8a123268f047d0b0b9319040506e2a3791298319254920500/calendar.ics")));
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException();
		}
	}

}
