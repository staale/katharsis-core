package io.katharsis.resource.mock.repository;

import io.katharsis.queryParams.QueryParams;
import io.katharsis.repository.RelationshipRepository;
import io.katharsis.resource.mock.models.Project;
import io.katharsis.resource.mock.models.Task;
import io.katharsis.resource.mock.repository.util.Relation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TaskToProjectRepository implements RelationshipRepository<Task, Long, Project, Long> {

    private static final ConcurrentMap<Relation<Task>, Integer> THREAD_LOCAL_REPOSITORY = new ConcurrentHashMap<>();

    @Override
    public void setRelation(Task source, Long targetId, String fieldName) {
        removeRelations(fieldName);
        if (targetId != null) {
            THREAD_LOCAL_REPOSITORY.put(new Relation<>(source, targetId, fieldName), 0);
        }
    }

    @Override
    public void setRelations(Task source, Iterable<Long> targetIds, String fieldName) {
        removeRelations(fieldName);
        if (targetIds != null) {
            for (Long targetId : targetIds) {
                THREAD_LOCAL_REPOSITORY.put(new Relation<>(source, targetId, fieldName), 0);
            }
        }
    }

    @Override
    public void addRelations(Task source, Iterable<Long> targetIds, String fieldName) {
        targetIds.forEach(targetId ->
                THREAD_LOCAL_REPOSITORY.put(new Relation<>(source, targetId, fieldName), 0)
        );
    }

    @Override
    public void removeRelations(Task source, Iterable<Long> targetIds, String fieldName) {
        targetIds.forEach(targetId -> {
            Iterator<Relation<Task>> iterator = THREAD_LOCAL_REPOSITORY.keySet().iterator();
            while (iterator.hasNext()) {
                Relation<Task> next = iterator.next();
                if (next.getFieldName().equals(fieldName) && next.getTargetId().equals(targetId)) {
                    iterator.remove();
                }
            }
        });
    }

    public void removeRelations(String fieldName) {
        Iterator<Relation<Task>> iterator = THREAD_LOCAL_REPOSITORY.keySet().iterator();
        while (iterator.hasNext()) {
            Relation<Task> next = iterator.next();
            if (next.getFieldName().equals(fieldName)) {
                iterator.remove();
            }
        }
    }

    @Override
    public Project findOneTarget(Long sourceId, String fieldName, QueryParams queryParams) {
        for (Relation<Task> relation : THREAD_LOCAL_REPOSITORY.keySet()) {
            if (relation.getSource().getId().equals(sourceId) &&
                relation.getFieldName().equals(fieldName)) {
                Project project = new Project();
                project.setId((Long) relation.getTargetId());
                return project;
            }
        }
        return null;
    }

    @Override
    public Iterable<Project> findManyTargets(Long sourceId, String fieldName, QueryParams queryParams) {
        List<Project> projects = new LinkedList<>();
        THREAD_LOCAL_REPOSITORY.keySet()
            .stream()
            .filter(relation -> relation.getSource()
                .getId()
                .equals(sourceId) && relation.getFieldName()
                .equals(fieldName))
            .forEach(relation -> {
                Project project = new Project();
                project.setId((Long) relation.getTargetId());
                projects.add(project);
            });
        return projects;
    }
}
