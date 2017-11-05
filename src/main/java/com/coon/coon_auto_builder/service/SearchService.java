package com.coon.coon_auto_builder.service;

import com.coon.coon_auto_builder.controller.dto.ResponseDTO;
import com.coon.coon_auto_builder.data.dao.BuildDAOService;
import com.coon.coon_auto_builder.data.dto.BuildDTO;
import com.coon.coon_auto_builder.data.dto.PackageVersionDTO;
import com.coon.coon_auto_builder.data.dto.RepositoryDTO;
import com.coon.coon_auto_builder.data.entity.Build;
import com.coon.coon_auto_builder.data.entity.PackageVersion;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class SearchService extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private BuildDAOService buildDao;

    @Async("searchExecutor")
    public CompletableFuture<ResponseDTO<List<BuildDTO>>> fetchBuilds(RepositoryDTO request) {
        LOGGER.debug("Fetch {}", request);
        List<Build> builds = findBuilds(request);
        Type listType = new TypeToken<List<BuildDTO>>() {
        }.getType();
        List<BuildDTO> found = modelMapper.map(builds, listType);
        return CompletableFuture.completedFuture(ok(found));
    }

    @Async("searchExecutor")
    public CompletableFuture<ResponseDTO<List<PackageVersionDTO>>> searchVersions(RepositoryDTO request) {
        LOGGER.debug("Search {}", request);
        List<Build> builds = findBuilds(request);
        Set<PackageVersion> versions = new HashSet<>();
        for(Build b : builds)
            versions.add(b.getPackageVersion());
        Type listType = new TypeToken<List<PackageVersionDTO>>() {
        }.getType();
        List<PackageVersionDTO> found = modelMapper.map(versions, listType);
        return CompletableFuture.completedFuture(ok(found));
    }

    @Async("searchExecutor")
    public CompletableFuture<ResponseDTO> findBuild(String build_id) {
        LOGGER.debug("Find {}", build_id);
        Optional<Build> find = buildDao.find(build_id);
        if (find.isPresent()) {
            BuildDTO dto = modelMapper.map(find.get(), BuildDTO.class);
            return CompletableFuture.completedFuture(ok(dto));
        }
        return CompletableFuture.completedFuture(fail("No build for id"));
    }

    private List<Build> findBuilds(RepositoryDTO request) {
        String[] splitted = request.getFullName().split("/");
        List<PackageVersionDTO> versions = request.getVersions();
        List<Build> builds = new ArrayList<>();
        if (versions.isEmpty()) {
            builds.addAll(buildDao.fetchByValues(splitted[1], splitted[0]));
        } else {
            for (PackageVersionDTO pv : versions) {
                builds.addAll(buildDao.fetchByValues(
                        splitted[1], splitted[0], pv.getRef(), pv.getErlVersion()));
            }
        }
        return builds;
    }
}